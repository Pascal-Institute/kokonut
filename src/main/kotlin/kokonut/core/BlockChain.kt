package kokonut.core

import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.GitHubFile
import kokonut.Policy
import kokonut.SQLite
import kokonut.URL.FUEL_NODE
import kokonut.URL.FULL_RAW_STORAGE
import kokonut.URL.FULL_STORAGE
import kokonut.Utility
import kokonut.Utility.Companion.sendHttpGetPolicy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class BlockChain(useDB: Boolean = true) {

    val sqlite = SQLite()

    private val chain: MutableList<Block> = mutableListOf()

    init {
        loadChainFromNetwork()

        println("Block Chain validity : ${isValid()}")

        if (useDB) {
            sqlite.insertChainIntoDatabase("kovault", chain)
        }

    }

    fun loadChainFromNetwork() = runBlocking {

        chain.clear()

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        try {

            val files: List<GitHubFile> = client.get(FULL_STORAGE).body()

            val jsonUrls = files.filter { it.type == "file" && it.name.endsWith(".json") }
                .map { "${FULL_RAW_STORAGE}${it.path}" }

            for (url in jsonUrls) {
                val response: HttpResponse = client.get(url)
                try {
                    val block : Block = Json.decodeFromString(response.body())
                    chain.add(block)
                } catch (e: Exception) {
                    println("JSON Passer Error: ${e.message}")
                }
            }

            sortByIndex()

        } catch (e: Exception) {
            println("Error! : ${e.message}")
        } finally {
            client.close()
        }
    }

    private fun sortByIndex() {
        chain.sortBy { it.index }
    }

    fun getGenesisBlock(): Block {
        return chain.first()
    }

    fun getLastBlock(): Block {
        return chain.last()
    }

    fun getTotalCurrencyVolume() : Double {

        var totalCurrencyVolume = 0.0

        chain.forEach {
            totalCurrencyVolume += it.data.reward
        }

        return Utility.truncate(totalCurrencyVolume)
    }

    fun mine(url : String, data: Data) : Block {

        val policy = sendHttpGetPolicy(FUEL_NODE)

        val lastBlock = getLastBlock()

        val version = Identity.version
        val index =  lastBlock.index + 1
        val previousHash = lastBlock.hash
        var timestamp = System.currentTimeMillis()
        val difficulty = policy.difficulty
        var nonce : Long = 0

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
        val fullNodeReward = Utility.sendHttpGetReward(url, miningBlock.index)

        if(data.reward != fullNodeReward){
            throw Exception("Reward Is Invalid...")
        }else{
            println("Reward Is Valid")
        }

        var miningHash = miningBlock.calculateHash()

        while(policy.difficulty > countLeadingZeros(miningHash)){
            timestamp = System.currentTimeMillis()
            nonce++

            miningBlock.timestamp = timestamp
            miningBlock.nonce = nonce
            miningHash = miningBlock.calculateHash()

            println("Nonce : $nonce")
        }

        return miningBlock
    }

    private fun countLeadingZeros(hash: String): Int {
        return hash.takeWhile { it == '0' }.length
    }

    fun addBlock(policy: Policy, block: Block) : Boolean{

        //Proven Of Work
        if(block.version != policy.version){
            return false
        }

        if(block.hash != block.calculateHash()){
            return false
        }

        //Proven done!
        chain.add(block)
        return true
    }

    fun isValid(): Boolean {
        for (i in chain.size - 1 downTo  1) {

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
}