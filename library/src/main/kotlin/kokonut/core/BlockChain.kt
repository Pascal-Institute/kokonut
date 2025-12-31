package kokonut.core

import java.net.URL
import kokonut.state.MiningState
import kokonut.util.*
import kokonut.util.API.Companion.getChain
import kokonut.util.API.Companion.getFullNodes
import kokonut.util.API.Companion.getGenesisBlock
import kokonut.util.FullNode
import kokonut.util.Utility.Companion.protocolVersion
import kokonut.util.Utility.Companion.truncate
import kotlinx.coroutines.runBlocking

class BlockChain {
    companion object {
        const val TICKER = "KNT"
        val database by lazy { SQLite() }

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
         * 1. Try to load from local DB (Persistence)
         * 2. If empty, try to bootstrap from peer
         * 3. If no peer, check if we should create Genesis (Bootstrap Node)
         */
        fun initialize(peerAddress: String? = null) {
            // 1. Try Load from DB
            loadChain()

            if (getChainSize() > 0) {
                println("✅ Blockchain loaded from local database. Size: ${getChainSize()}")
                scanFuelNodes()
                return
            }

            // 2. If DB empty, try Bootstrap
            val peer = peerAddress ?: knownPeer

            if (peer != null) {
                try {
                    bootstrapFromPeer(peer)
                    return
                } catch (e: Exception) {
                    println("⚠️ Bootstrap failed: ${e.message}")
                }
            }

            // 3. If no peer or bootstrap failed, are we the Genesis Node?
            // For simplicity, if DB is empty and no peer, we assume this is a fresh network start
            println("🌱 No existing chain and no peer found. Creating Genesis Block...")
            val genesis = GenesisGenerator.createGenesisBlock()
            database.insert(genesis)

            // Also create bootstrap fuel registration for self?
            // This is tricky without knowing our own address.
            // For now, we just create Genesis. Detailed Fuel registration usually happens via
            // separate tool or setup.

            syncChain()
            scanFuelNodes()
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
                // Determine Fuel Node to query
                val fuelNodeUrl =
                        try {
                            getRandomFuelNode()
                        } catch (e: Exception) {
                            println("⚠️ Cannot load Full Nodes: ${e.message}")
                            return
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
                }
            } catch (e: Exception) {
                println("❌ Error loading Full Nodes: ${e.message}")
            }
        }

        private fun loadChain() {
            // Only sync from local DB
            // External sync is handled by bootstrapFromPeer or explicit sync calls
            syncChain()
            println("Block Chain validity : ${isValid()}")
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
            val totalCurrencyVolume = cachedChain?.sumOf { it.data.reward } ?: 0.0
            return truncate(totalCurrencyVolume)
        }

        /**
         * PoS Validation: Replaces the traditional PoW mining Validators are selected based on
         * their stake, not computational power
         */
        fun validate(wallet: Wallet, data: Data): Block {
            wallet.miningState = MiningState.MINING // TODO: Rename states to VALIDATING

            // Refresh Full Nodes and sync chain
            loadFullNodes()
            if (fullNodes.isNotEmpty()) {
                loadChainFromFullNode(URL(fullNode.address))
            }

            if (!isValid()) {
                wallet.miningState = MiningState.FAILED
                throw IllegalStateException("Chain is Invalid. Stop Validation...")
            }

            // Check if wallet is registered as validator
            val validatorAddress = Utility.calculateHash(wallet.publicKey)
            val validator = validatorPool.getValidator(validatorAddress)

            if (validator == null || !validator.isActive) {
                wallet.miningState = MiningState.FAILED
                throw IllegalStateException(
                        "Wallet is not registered as active validator. Stake KNT to become a validator."
                )
            }

            // Select validator for this block (probabilistic based on stake)
            val selectedValidator = validatorPool.selectValidator()
            if (selectedValidator == null || selectedValidator.address != validatorAddress) {
                wallet.miningState = MiningState.FAILED
                throw IllegalStateException(
                        "Validator not selected for this block. Try again on next block."
                )
            }

            val lastBlock = getLastBlock()

            val version = protocolVersion
            val index = lastBlock.index + 1
            val previousHash = lastBlock.hash
            val timestamp = System.currentTimeMillis()

            // Calculate reward (transaction fees in PoS, not block rewards)
            data.reward = Utility.setReward(index) * ValidatorPool.VALIDATOR_REWARD_PERCENTAGE

            // Create validator signature to prove block authenticity
            val blockData = "$version$index$previousHash$timestamp$data"
            val signature = Wallet.signData(blockData.toByteArray(), wallet.privateKey)
            val validatorSignature = signature.fold("") { str, it -> str + "%02x".format(it) }

            val validationBlock =
                    Block(
                            version = version,
                            index = index,
                            previousHash = previousHash,
                            timestamp = timestamp,
                            data = data,
                            validatorSignature = validatorSignature,
                            hash = ""
                    )

            validationBlock.hash = validationBlock.calculateHash()

            // Reward the validator
            validatorPool.rewardValidator(validatorAddress, data.reward)

            println("✅ Block #$index validated by: $validatorAddress")
            println("   Reward: ${data.reward} KNT")
            println("   Total Validators: ${validatorPool.getActiveValidators().size}")

            wallet.miningState = MiningState.MINED // TODO: Rename to VALIDATED

            return validationBlock
        }

        fun getChain(): MutableList<Block> {
            return database.fetch()
        }

        fun getChainSize(): Long {
            return (cachedChain!!.size).toLong()
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
            }
            return true
        }

        private fun syncChain() {
            cachedChain = database.fetch().sortedBy { it.index }
        }
    }
}
