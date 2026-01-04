package kokonut.core

import java.net.URL
import kokonut.state.ValidatorState
import kokonut.util.*
import kokonut.util.API.Companion.detectPeerType
import kokonut.util.API.Companion.getChain
import kokonut.util.API.Companion.getFullNodes
import kokonut.util.API.Companion.getGenesisBlock
import kokonut.util.FullNode
import kokonut.util.Utility.Companion.truncate
import kotlinx.coroutines.runBlocking

class BlockChain {
    companion object {
        const val TICKER = "KNT"
        private const val TREASURY_ADDRESS_ENV = "KOKONUT_TREASURY_ADDRESS"
        private const val DEFAULT_TREASURY_ADDRESS = "KOKONUT_TREASURY"
        private const val STAKE_VAULT_ADDRESS_ENV = "KOKONUT_STAKE_VAULT_ADDRESS"
        private const val DEFAULT_STAKE_VAULT_ADDRESS = "KOKONUT_STAKE_VAULT"
        var database = SQLite()

        // Known peer for bootstrapping
        // Set via environment variable: KOKONUT_PEER=http://known-node-address
        var knownPeer: String? = System.getenv("KOKONUT_PEER")

        var fullNode = FullNode("", "")
        var fullNodes: List<FullNode> = emptyList()
        private var cachedChain: List<Block>? = null

        // PoS Validator Pool
        val validatorPool = ValidatorPool()

        // Cached Fuel Nodes (scanned from blockchain)
        private var cachedFuelNodes: List<FuelNodeInfo> = emptyList()

        /**
         * Initialize blockchain
         * @param nodeType The type of the running node (FUEL, FULL, LIGHT)
         * @param peerAddress Optional specific peer address to bootstrap from
         *
         * Rules:
         * 1. Try to load from local DB (Persistence).
         * 2. If empty, try to bootstrap from peer.
         * 3. If no peer/bootstrap fails:
         * ```
         *    - FUEL Node: Creates new Genesis Block (starts network).
         *    - FULL/LIGHT Node: THROWS EXCEPTION (must connect to existing network).
         * ```
         */
        fun initialize(nodeType: NodeType, peerAddress: String? = null) {
            // 1. Try Load from DB
            loadChain()

            if (getChainSize() > 0) {
                println("✅ Blockchain loaded from local database. Size: ${getChainSize()}")
                scanFuelNodes()

                // Safe upgrade path: if this is a Fuel node with only Genesis, ensure the
                // one-time treasury mint block exists.
                if (nodeType == NodeType.FUEL) {
                    ensureGenesisTreasuryMintIfNeeded()
                }
                return
            }

            // 2. If DB empty, try Bootstrap
            val peer = peerAddress ?: knownPeer

            if (peer != null) {
                try {
                    // Auto-discover peer type and setup connections
                    if (nodeType == NodeType.FULL) {
                        discoverAndConnectPeers(peer)
                    } else {
                        bootstrapFromPeer(peer)
                        knownPeer = peer
                    }
                    return
                } catch (e: Exception) {
                    println("❌ Bootstrap failed from peer: $peer")
                    println("Error: ${e.message}")

                    if (nodeType != NodeType.FUEL) {
                        throw IllegalStateException(
                                "Failed to bootstrap from configured peer. Check network connection or peer address."
                        )
                    }
                    // For Fuel Node, we might fall back to Genesis creation if peer fails (e.g.
                    // self-referencing)
                }
            }

            // 3. Handle Fresh Start (No Chain, No Peer)
            if (nodeType == NodeType.FUEL) {
                println(
                        "🌱 Fuel Node: No existing chain and no peer found. Creating Genesis Block..."
                )
                val genesis = GenesisGenerator.createGenesisBlock()
                database.insert(genesis)

                // Record initial treasury funding (external supply injection)
                val mintBlock =
                        GenesisGenerator.createGenesisTreasuryMintBlock(
                                treasuryAddress = getTreasuryAddress(),
                                previousHash = genesis.hash,
                                index = 1,
                                amount = GenesisGenerator.GENESIS_TREASURY_MINT_AMOUNT
                        )
                database.insert(mintBlock)

                refreshFromDatabase()
            } else {
                throw IllegalStateException(
                        "❌ ${nodeType} Node CANNOT create Genesis Block.\n" +
                                "MUST be connected to a Fuel Node or Peer to join the network.\n" +
                                "Please set KOKONUT_PEER environment variable."
                )
            }
        }

        private val discoveryLock = Any()

        /**
         * Discovers peer type and sets up appropriate connections for FullNode.
         *
         * Scenarios:
         * 1. Peer is FuelNode -> Connect to FuelNode + discover and connect to other FullNodes
         * 2. Peer is FullNode -> Connect to that FullNode + discover FuelNode from it
         *
         * @param peerAddress The initial peer address to connect to
         */
        private fun discoverAndConnectPeers(peerAddress: String) {
            synchronized(discoveryLock) {
                println("🔍 Detecting peer type for: $peerAddress")

                val peerUrl = URL(peerAddress)
                val peerType = peerUrl.detectPeerType()

                when (peerType) {
                    "FUEL" -> {
                        println("✅ Detected FUEL Node: $peerAddress")
                        // Bootstrap from FuelNode
                        bootstrapFromPeer(peerAddress)
                        knownPeer = peerAddress

                        // Try to discover other FullNodes
                        try {
                            val otherFullNodes = peerUrl.getFullNodes()
                            if (otherFullNodes.isNotEmpty()) {
                                println("🔗 Found ${otherFullNodes.size} other Full Node(s):")
                                otherFullNodes.take(3).forEach { node ->
                                    println("   - ${node.address}")
                                }

                                // Set the first available FullNode as preferred peer
                                val preferredFullNode = otherFullNodes.firstOrNull()
                                if (preferredFullNode != null) {
                                    fullNode = preferredFullNode
                                    println(
                                            "✨ Set preferred Full Node peer: ${preferredFullNode.address}"
                                    )
                                }
                            } else {
                                println("ℹ️ No other Full Nodes found yet. You are the first!")
                            }
                        } catch (e: Exception) {
                            println("⚠️ Could not fetch other Full Nodes: ${e.message}")
                        }
                    }
                    "FULL" -> {
                        println("✅ Detected FULL Node: $peerAddress")
                        // Bootstrap from FullNode
                        bootstrapFromPeer(peerAddress)

                        // Try to discover FuelNode from the blockchain
                        try {
                            val fuelNodes = getFuelNodes()
                            if (fuelNodes.isNotEmpty()) {
                                val fuelNodeAddress = fuelNodes.first().address
                                knownPeer = fuelNodeAddress
                                println("✨ Auto-discovered Fuel Node: $fuelNodeAddress")

                                // Also discover other FullNodes from FuelNode
                                try {
                                    val otherFullNodes = URL(fuelNodeAddress).getFullNodes()
                                    if (otherFullNodes.isNotEmpty()) {
                                        println(
                                                "🔗 Found ${otherFullNodes.size} Full Node(s) via Fuel Node"
                                        )
                                    }
                                } catch (e: Exception) {
                                    println(
                                            "⚠️ Could not fetch Full Nodes from Fuel Node: ${e.message}"
                                    )
                                }
                            } else {
                                println("⚠️ No Fuel Nodes found in blockchain")
                            }
                        } catch (e: Exception) {
                            println("⚠️ Could not auto-discover Fuel Node: ${e.message}")
                        }
                    }
                    else -> {
                        println(
                                "⚠️ Could not determine peer type, proceeding with standard bootstrap"
                        )
                        bootstrapFromPeer(peerAddress)
                        knownPeer = peerAddress
                    }
                }
            }
        }

        fun getTreasuryAddress(): String {
            return System.getenv(TREASURY_ADDRESS_ENV)?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_TREASURY_ADDRESS
        }

        fun getStakeVaultAddress(): String {
            return System.getenv(STAKE_VAULT_ADDRESS_ENV)?.takeIf { it.isNotBlank() }
                    ?: DEFAULT_STAKE_VAULT_ADDRESS
        }

        private fun hasGenesisTreasuryMint(chain: List<Block>): Boolean {
            return chain.any { block ->
                block.data.transactions.any { tx ->
                    tx.transaction == "GENESIS_MINT" && tx.receiver == getTreasuryAddress()
                }
            }
        }

        /**
         * Ensures the Genesis treasury mint block exists exactly once.
         *
         * Guardrails:
         * - Never inserts if already present.
         * - Only auto-inserts when the chain is effectively fresh (Genesis-only).
         */
        private fun ensureGenesisTreasuryMintIfNeeded() {
            val chain = cachedChain ?: emptyList()
            if (chain.isEmpty()) return
            if (hasGenesisTreasuryMint(chain)) return

            // Only safe to auto-mutate a brand-new chain.
            if (chain.size != 1L.toInt() || chain.first().index != 0L) {
                println(
                        "⚠️ Genesis treasury mint is missing but chain is not fresh; skipping auto-insert to avoid rewriting history."
                )
                return
            }

            val genesis = chain.first()
            val mintBlock =
                    GenesisGenerator.createGenesisTreasuryMintBlock(
                            treasuryAddress = getTreasuryAddress(),
                            previousHash = genesis.hash,
                            index = 1,
                            amount = GenesisGenerator.GENESIS_TREASURY_MINT_AMOUNT
                    )

            database.insert(mintBlock)
            refreshFromDatabase()
            println(
                    "🏦 Inserted one-time Genesis treasury mint: ${GenesisGenerator.GENESIS_TREASURY_MINT_AMOUNT} KNT -> ${getTreasuryAddress()}"
            )
        }

        /**
         * Bootstrap from a known peer (Peer Discovery) Downloads Genesis and blockchain, then scans
         * for Fuel Nodes
         */
        fun bootstrapFromPeer(peerAddress: String) {
            try {
                println("🔍 Bootstrapping from peer: $peerAddress")

                // 1. Download Genesis Block from known peer
                val genesis = URL(peerAddress).getGenesisBlock()
                println("✅ Genesis Block downloaded: ${genesis.hash}")

                // 2. Download entire blockchain
                val chain = URL(peerAddress).getChain()
                println("✅ Blockchain downloaded: ${chain.size} blocks")

                // 3. Save to database
                chain.forEach { block ->
                    if (block !in getChain()) {
                        database.insert(block)
                    }
                }
                syncChain()

                // 4. Scan for Fuel Nodes
                val fuels = scanFuelNodes()
                println("✅ Found ${fuels.size} Fuel Nodes in blockchain")
                fuels.forEach { fuel -> println("   - ${fuel.address} (stake: ${fuel.stake} KNT)") }

                println("🎉 Bootstrap complete! Connected to Kokonut network.")
            } catch (e: Exception) {
                println("❌ Bootstrap failed: ${e.message}")
                println("   Make sure the peer address is correct and accessible.")
                throw e
            }
        }

        /**
         * Scan blockchain to find all registered Fuel Nodes This replaces hardcoded Fuel Node list
         */
        fun scanFuelNodes(): List<FuelNodeInfo> {
            val fuelNodes = mutableListOf<FuelNodeInfo>()
            val chain = cachedChain ?: emptyList()

            chain.forEach { block ->
                when (block.data.type) {
                    BlockDataType.FUEL_REGISTRATION -> {
                        block.data.fuelNodeInfo?.let { fuelNodes.add(it) }
                    }
                    BlockDataType.FUEL_REMOVAL -> {
                        block.data.fuelNodeInfo?.let { removed ->
                            fuelNodes.removeIf { it.address == removed.address }
                        }
                    }
                    else -> {}
                }
            }

            cachedFuelNodes = fuelNodes
            return fuelNodes
        }

        /** Get current Fuel Nodes from blockchain */
        fun getFuelNodes(): List<FuelNodeInfo> {
            if (cachedFuelNodes.isEmpty()) {
                scanFuelNodes()
            }
            return cachedFuelNodes
        }

        /** Get network rules from Genesis Block */
        fun getNetworkRules(): NetworkRules {
            val genesis = getGenesisBlock()
            return genesis.data.networkRules ?: NetworkRules() // Default rules if not set
        }

        /** Check if an address is a registered Fuel Node */
        fun isFuelNode(address: String): Boolean {
            return getFuelNodes().any { it.address == address }
        }

        /**
         * Get a random Fuel Node from blockchain Throws exception if no Fuel Nodes found (network
         * not bootstrapped)
         */
        fun getRandomFuelNode(): URL {
            val fuelNodes = getFuelNodes()
            if (fuelNodes.isEmpty()) {
                throw IllegalStateException(
                        "No Fuel Nodes found. Please bootstrap from a known peer first.\n" +
                                "Use: BlockChain.initialize(\"http://known-node-address\")\n" +
                                "Or set environment variable: KOKONUT_PEER=http://known-node-address"
                )
            }
            return URL(fuelNodes.random().address)
        }

        /** Get primary (bootstrap) Fuel Node Returns the first registered Fuel Node */
        fun getPrimaryFuelNode(): URL {
            val fuelNodes = getFuelNodes()
            if (fuelNodes.isEmpty()) {
                throw IllegalStateException(
                        "No Fuel Nodes found. Please bootstrap from a known peer first."
                )
            }
            val bootstrap = fuelNodes.find { it.isBootstrap }
            return URL(bootstrap?.address ?: fuelNodes.first().address)
        }

        internal fun loadFullNodes() {
            try {
                // If we have a known peer (e.g., LightNode connected to FullNode), use it directly
                val fuelNodeUrl =
                        if (!knownPeer.isNullOrBlank()) {
                            URL(knownPeer)
                        } else {
                            try {
                                getRandomFuelNode()
                            } catch (e: Exception) {
                                println("⚠️ Cannot load Full Nodes: ${e.message}")
                                return
                            }
                        }

                runBlocking { fullNodes = fuelNodeUrl.getFullNodes() }

                if (fullNodes.isNotEmpty()) {
                    var maxSize = 0
                    var bestNode = fullNodes[0]

                    for (node in fullNodes) {
                        try {
                            val size = URL(node.address).getChain().size
                            if (size > maxSize) {
                                maxSize = size
                                bestNode = node
                            }
                        } catch (e: Exception) {
                            println("⚠️ Failed to check node ${node.address}: ${e.message}")
                        }
                    }
                    fullNode = bestNode
                } else {
                    // If no Full Nodes returned, use knownPeer as fallback
                    if (!knownPeer.isNullOrBlank()) {
                        fullNode = FullNode("known-peer", knownPeer!!)
                    }
                }
            } catch (e: Exception) {
                println("❌ Error loading Full Nodes: ${e.message}")
                // Last resort: use knownPeer if available
                if (!knownPeer.isNullOrBlank()) {
                    fullNode = FullNode("known-peer", knownPeer!!)
                }
            }
        }

        private fun loadChain() {
            // Only sync from local DB
            // External sync is handled by bootstrapFromPeer or explicit sync calls
            syncChain()
            println("Block Chain validity : ${isValid()}")
        }

        /**
         * Refresh in-memory caches from the persisted chain in SQLite.
         *
         * This is important because blocks are inserted directly into the database from HTTP
         * routes, but most chain reads (e.g., getLastBlock) rely on the in-memory cached chain.
         */
        fun refreshFromDatabase() {
            syncChain()
            scanFuelNodes()
        }

        // loadChainFromFuelNode removed - replaced by bootstrapFromPeer

        fun loadChainFromFullNode(url: URL) = runBlocking {
            try {
                val chainFromFullNode = runBlocking { url.getChain() }
                val chain = getChain()

                chainFromFullNode.forEach { block ->
                    if (block !in chain) {
                        database.insert(block)
                    }
                }
            } catch (e: Exception) {
                println("Aborted : ${e.message}")
            }

            syncChain()

            println("Block Chain validity : ${isValid()}")
        }

        // Parameterless loadChainFromFullNode removed - use specific URL version

        fun isRegistered(): Boolean {

            if (fullNode.id == "") {
                return false
            }

            loadFullNodes()

            if (fullNodes.isNotEmpty()) {
                return fullNodes.contains(fullNode)
            }

            return false
        }

        fun getGenesisBlock(): Block =
                cachedChain?.firstOrNull() ?: throw IllegalStateException("Chain is Empty")

        fun getLastBlock(): Block =
                cachedChain?.lastOrNull() ?: throw IllegalStateException("Chain is Empty")

        fun getTotalCurrencyVolume(): Double {
            val chain = cachedChain ?: emptyList()
            val genesisMinted =
                    chain.sumOf { block ->
                        block.data.transactions.filter { it.transaction == "GENESIS_MINT" }.sumOf {
                            it.remittance
                        }
                    }
            // B-model: rewards are treasury-paid transfers (not inflation), so total supply is
            // determined by genesis/external mints.
            return truncate(genesisMinted)
        }

        /**
         * Compute account-like balance from on-chain transactions.
         *
         * Rules:
         * - Receiver gains `remittance`
         * - Sender loses `remittance + commission`
         * - Commission is not credited elsewhere (commission is 0.0 in current flows)
         */
        fun getBalance(address: String): Double {
            val chain = cachedChain ?: emptyList()
            var balance = 0.0
            chain.forEach { block ->
                block.data.transactions.forEach { tx ->
                    if (tx.receiver == address) {
                        balance += tx.remittance
                    }
                    if (tx.sender == address) {
                        balance -= (tx.remittance + tx.commission)
                    }
                }
            }
            return truncate(balance)
        }

        fun getTreasuryBalance(): Double {
            return getBalance(getTreasuryAddress())
        }

        /**
         * PoS Validation: Replaces the traditional PoW mining Validators are selected based on
         * their stake, not computational power
         */
        fun validate(wallet: Wallet, data: Data): Block {
            wallet.validationState = ValidatorState.VALIDATING

            // Refresh Full Nodes and sync chain
            loadFullNodes()
            if (fullNodes.isNotEmpty()) {
                loadChainFromFullNode(URL(fullNode.address))
            }

            if (!isValid()) {
                wallet.validationState = ValidatorState.FAILED
                throw IllegalStateException("Chain is Invalid. Stop Validation...")
            }

            // Check if wallet is registered as validator
            val validatorAddress = Utility.calculateHash(wallet.publicKey)
            val validator = validatorPool.getValidator(validatorAddress)

            if (validator == null || !validator.isActive) {
                wallet.validationState = ValidatorState.FAILED
                throw IllegalStateException(
                        "Wallet is not registered as active validator. Stake KNT to become a validator."
                )
            }

            // Select validator for this block (probabilistic based on stake)
            val lastBlock = getLastBlock()
            val index = lastBlock.index + 1
            val previousHash = lastBlock.hash

            // Deterministic Seed: previousHash (as Long) + index
            // Using hashCode() of string for simplicity, in production should use byte conversion
            // of hash
            val seed = previousHash.hashCode().toLong() + index

            val selectedValidator = validatorPool.selectValidator(seed)
            if (selectedValidator == null || selectedValidator.address != validatorAddress) {
                wallet.validationState = ValidatorState.FAILED
                throw IllegalStateException(
                        "Validator not selected for this block. Try again on next block."
                )
            }

            val timestamp = System.currentTimeMillis()

            // Calculate validator reward (paid from treasury as an on-chain tx)
            val rewardAmount = Utility.setReward(index) * ValidatorPool.VALIDATOR_REWARD_PERCENTAGE
            val treasuryAddress = getTreasuryAddress()
            val treasuryBalance = getTreasuryBalance()

            // IMPORTANT: Use block timestamp for transaction to ensure hash consistency
            val rewardTx =
                    if (treasuryBalance >= rewardAmount && rewardAmount > 0.0) {
                        Transaction(
                                transaction = ValidatorPool.VALIDATOR_REWARD_TX,
                                sender = treasuryAddress,
                                receiver = validatorAddress,
                                remittance = truncate(rewardAmount),
                                commission = 0.0,
                                timestamp = timestamp
                        )
                    } else {
                        null
                    }

            data.reward = 0.0

            val rewardTransactions =
                    if (rewardTx != null) {
                        data.transactions + rewardTx
                    } else {
                        data.transactions
                    }

            // Set validator address in data (critical for validator tracking)
            val validatedData =
                    data.copy(validator = validatorAddress, transactions = rewardTransactions)

            // Create validator signature to prove block authenticity
            val blockData = "$index$previousHash$timestamp$validatedData"
            val signature = Wallet.signData(blockData.toByteArray(), wallet.privateKey)
            val validatorSignature = signature.fold("") { str, it -> str + "%02x".format(it) }

            val validationBlock =
                    Block(
                            index = index,
                            previousHash = previousHash,
                            timestamp = timestamp,
                            data = validatedData, // Use validatedData with validator set
                            validatorSignature = validatorSignature,
                            hash = ""
                    )

            validationBlock.hash = validationBlock.calculateHash()

            println("✅ Block #$index validated by: $validatorAddress")
            println("   Reward: ${rewardTx?.remittance ?: 0.0} KNT")
            println("   Total Validators: ${validatorPool.getActiveValidators().size}")

            wallet.validationState = ValidatorState.VALIDATED

            return validationBlock
        }

        fun getChain(): MutableList<Block> {
            return database.fetch()
        }

        fun getChainSize(): Long {
            return (cachedChain?.size ?: 0).toLong()
        }

        fun isValid(): Boolean {
            val chain = cachedChain ?: return false
            for (i in chain.size - 1 downTo 1) {

                val currentBlock = chain[i]
                val previousBlock = chain[i - 1]

                if (!currentBlock.isValid()) {
                    return false
                }

                if (currentBlock.previousHash != previousBlock.hash) {
                    return false
                }

                // PoS Validator verification: Ensure the block was created by the selected
                // validator
                val seed = previousBlock.hash.hashCode().toLong() + currentBlock.index
                // Lookup stake history at the time of previous block (limitIndex = i-1)
                val expectedValidator =
                        validatorPool.selectValidator(seed, limitIndex = previousBlock.index)

                if (expectedValidator == null ||
                                currentBlock.data.validator != expectedValidator.address
                ) {
                    // System blocks (e.g. Genesis/Bootstrap/Staking) exceptions
                    val systemValidators =
                            listOf("GENESIS", "BOOTSTRAP", "ONBOARDING", "STAKE_LOCK", "UNSTAKE")
                    if (currentBlock.data.validator in systemValidators) {
                        continue
                    }

                    println("❌ Invalid Validator for Block #${currentBlock.index}")
                    println("   Expected: ${expectedValidator?.address}")
                    println("   Actual:   ${currentBlock.data.validator}")
                    return false
                }
            }
            return true
        }

        private fun syncChain() {
            cachedChain = database.fetch().sortedBy { it.index }
        }
    }
}
