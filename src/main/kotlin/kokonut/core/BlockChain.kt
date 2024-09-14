package kokonut.core

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.state.MiningState
import kokonut.state.Node
import kokonut.util.GitHubFile
import kokonut.URLBook
import kokonut.util.SQLite
import kokonut.URLBook.GENESIS_NODE
import kokonut.URLBook.GENESIS_RAW_NODE
import kokonut.URLBook.POLICY_NODE
import kokonut.URLBook.fullNodes
import kokonut.util.Wallet
import kokonut.util.API.Companion.getChain
import kokonut.util.API.Companion.getPolicy
import kokonut.util.API.Companion.getReward
import kokonut.util.API.Companion.startMining
import kokonut.util.Utility
import kokonut.util.Utility.Companion.truncate
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.net.URL

class BlockChain(val node: Node = Node.LIGHT, val url: URL = URLBook.FULL_NODE_0) {

    var miningState = MiningState.READY
    val database = SQLite()
    private var cachedChain: List<Block>? = null

    init {
        when (node) {
            Node.FULL -> {
                if (fullNodes.isEmpty()) {
                    loadChainFromGenesisNode()
                } else {
                    loadChainFromFullNode()
                }
            }

            Node.LIGHT -> loadChainFromFullNode()
        }
    }

    fun loadChainFromGenesisNode() = runBlocking {

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        try {
            val files: List<GitHubFile> = client.get(GENESIS_NODE).body()

            val jsonUrls = files.filter { it.type == "file" && it.name.endsWith(".json") }
                .map { "${GENESIS_RAW_NODE}${it.path}" }

            for (url in jsonUrls) {
                val response: HttpResponse = client.get(url)
                try {
                    val block: Block = Json.decodeFromString(response.body())
                    database.insert(block)
                } catch (e: Exception) {
                    println("JSON Passer Error: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error! : ${e.message}")
        } finally {
            client.close()
        }

        syncChain()

        println("Block Chain validity : ${isValid()}")
    }

    fun loadChainFromFullNode() = runBlocking {
        try {
            val newChain = url.getChain()
            val chain = database.fetch()

            newChain.forEach { newBlock ->
                if (newBlock !in chain) {
                    database.insert(newBlock)
                }
            }
        } catch (e: Exception) {
            println("Aborted : ${e.message}")
        }
        syncChain()

        println("Block Chain validity : ${isValid()}")
    }

    fun getGenesisBlock(): Block = cachedChain?.firstOrNull() ?: throw IllegalStateException("Chain is Empty")

    fun getLastBlock(): Block = cachedChain?.lastOrNull() ?: throw IllegalStateException("Chain is Empty")

    fun getTotalCurrencyVolume(): Double {
        val totalCurrencyVolume = cachedChain?.sumOf { it.data.reward } ?: 0.0
        return truncate(totalCurrencyVolume)
    }

    fun mine(wallet: Wallet, data: Data): Block {
        miningState = MiningState.MINING

        loadChainFromFullNode()

        url.startMining(wallet.publicKeyFile)

        if (!isValid()) {
            miningState = MiningState.FAILED
            throw IllegalStateException("Chain is Invalid. Stop Mining...")
        }

        val policy = POLICY_NODE.getPolicy()

        val lastBlock = getLastBlock()

        val version = Identity.protocolVersion
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
        val fullNodeReward = url.getReward(miningBlock.index)

        if (data.reward != fullNodeReward) {
            miningState = MiningState.FAILED
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