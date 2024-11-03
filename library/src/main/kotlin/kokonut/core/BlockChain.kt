package kokonut.core
import kokonut.state.MiningState
import kokonut.util.*
import kokonut.util.API.Companion.getChain
import kokonut.util.API.Companion.getFullNodes
import kokonut.util.API.Companion.getGenesisBlock
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.getReward
import kokonut.util.API.Companion.startMining
import kokonut.util.Utility.Companion.truncate
import kokonut.util.FullNode
import kotlinx.coroutines.runBlocking
import java.net.URL

class BlockChain {
    companion object {
        const val TICKER = "KNT"
        val database = SQLite()
        val FUEL_NODE = URL("https://kokonut-fuelnode.onrender.com")

        var fullNode = FullNode("", "")

        var fullNodes: List<FullNode> = emptyList()

        private var cachedChain: List<Block>? = null

        init {
            loadFullNodes()
            loadChain()
        }

        internal fun loadFullNodes() {
            var maxSize = 0
            var fullNodeChainSize = 0

            runBlocking {
                fullNodes = FUEL_NODE.getFullNodes()
            }

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

        fun getGenesisBlock(): Block = cachedChain?.firstOrNull() ?: throw IllegalStateException("Chain is Empty")

        fun getLastBlock(): Block = cachedChain?.lastOrNull() ?: throw IllegalStateException("Chain is Empty")

        fun getTotalCurrencyVolume(): Double {
            val totalCurrencyVolume = cachedChain?.sumOf { it.data.reward } ?: 0.0
            return truncate(totalCurrencyVolume)
        }

        fun mine(wallet: Wallet, data: Data): Block {
            wallet.miningState = MiningState.MINING

            loadChainFromFullNode()

            URL(fullNode.address).startMining(wallet.publicKeyFile)

            if (!isValid()) {
                wallet.miningState = MiningState.FAILED
                throw IllegalStateException("Chain is Invalid. Stop Mining...")
            }

            val policy = FUEL_NODE.getPolicy()

            val lastBlock = getLastBlock()

            val version = Version.protocolVersion
            val index = lastBlock.index + 1
            val previousHash = lastBlock.hash
            var timestamp = System.currentTimeMillis()
            val difficulty = policy.difficulty
            var nonce: Long = 0

            val miningBlock = Block(
                version = version,
                index = index,
                previousHash = previousHash,
                timestamp = timestamp,
                data = data,
                difficulty = difficulty,
                nonce = nonce,
                hash = ""
            )

            data.reward = Utility.setReward(miningBlock.index)
            val fullNodeReward = URL(fullNode.address).getReward(miningBlock.index)

            if (data.reward != fullNodeReward) {
                wallet.miningState = MiningState.FAILED
                throw Exception("Reward Is Invalid...")
            } else {
                println("Reward Is Valid")
            }

            var miningHash = miningBlock.calculateHash()

            while (policy.difficulty > countLeadingZeros(miningHash)) {
                timestamp = System.currentTimeMillis()
                nonce++

                miningBlock.timestamp = timestamp
                miningBlock.nonce = nonce
                miningHash = miningBlock.calculateHash()

                println("Nonce : $nonce")
            }

            return miningBlock
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

        private fun countLeadingZeros(hash: String): Int {
            return hash.takeWhile { it == '0' }.length
        }

        private fun syncChain() {
            cachedChain = database.fetch().sortedBy { it.index }
        }
    }
}
