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
        val FUEL_NODE = URL("http://kokonut-fuel.duckdns.org")

        var fullNode = FullNode("", "")
        var fullNodes: List<FullNode> = emptyList()
        private var cachedChain: List<Block>? = null

        // PoS Validator Pool
        val validatorPool = ValidatorPool()

        fun initialize() {
            loadFullNodes()
            loadChain()
        }

        internal fun loadFullNodes() {
            var maxSize = 0
            var fullNodeChainSize = 0

            runBlocking { fullNodes = FUEL_NODE.getFullNodes() }

            for (it in fullNodes) {
                fullNode = fullNodes[0]
                fullNodeChainSize = URL(it.address).getChain().size
                if (fullNodeChainSize > maxSize) {
                    fullNode = it
                    maxSize = fullNodeChainSize
                }
            }
        }

        private fun loadChain() {
            if (fullNodes.isEmpty()) {
                loadChainFromFuelNode()
            } else {
                loadChainFromFullNode()
            }
        }

        fun loadChainFromFuelNode() = runBlocking {
            try {
                val genesisBlock = runBlocking { FUEL_NODE.getGenesisBlock() }
                val chain = getChain()
                val blockChain = mutableListOf<Block>()
                blockChain.add(genesisBlock)

                blockChain.forEach { block ->
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

        fun loadChainFromFullNode() {
            try {
                val chainFromFullNode = URL(fullNode.address).getChain()
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

            loadChainFromFullNode()

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
