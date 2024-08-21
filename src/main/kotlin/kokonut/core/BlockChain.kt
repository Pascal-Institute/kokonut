package kokonut.core

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.GitHubFile
import kokonut.core.Identity.ticker
import kokonut.Policy
import kokonut.URL.FUEL_NODE
import kokonut.URL.FULL_RAW_STORAGE
import kokonut.URL.FULL_STORAGE
import kokonut.core.Block.Companion.calculateHash
import kokonut.Utility.Companion.sendHttpGetPolicy
import kotlinx.coroutines.processNextEventInCurrentThread
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class BlockChain {

    private val chain: MutableList<Block> = mutableListOf()
    private val genesisBlockDifficulty = 32
    private val genesisVersion = 0

    init {
        loadChainFromNetwork()

        println("Block Chain validity : ${isValid()}")
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
                    val updatedBlock = if(block.index.toInt() == 0)
                    {block.copy(
                        version = block.version ?: genesisVersion,
                        difficulty = block.difficulty ?: genesisBlockDifficulty
                    )}else{
                        block
                    }

                    chain.add(updatedBlock)
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
            if(it.version!!>= 3){
                totalCurrencyVolume += it.data.reward
            }
        }

        return totalCurrencyVolume
    }

    fun mine(blockData: BlockData) : Block {

        val policy = sendHttpGetPolicy(FUEL_NODE)

        val lastBlock = getLastBlock()

        val version = Identity.version
        val index =  lastBlock.index + 1
        val previousHash = lastBlock.hash
        var timestamp = System.currentTimeMillis()
        val data = blockData
        val difficulty = policy.difficulty
        var nonce : Long = 0

        var miningBlock = Block(
            version = version,
            index = index,
            previousHash = lastBlock.hash,
            timestamp=timestamp,
            data = blockData,
            difficulty = difficulty,
            nonce = nonce,
            hash = ""
        )
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

        if(block.data.reward != policy.reward){
            return false
        }

        val calculatedHash = block.calculateHash()
        if(block.hash != calculatedHash){
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

            val calculatedHash : String = when (currentBlock.version) {
                1 -> {
                    calculateHash(
                        previousBlock.version!!,
                        previousBlock.index,
                        previousBlock.previousHash,
                        currentBlock.timestamp,
                        previousBlock.data.ticker,
                        previousBlock.data,
                        previousBlock.difficulty!!,
                        currentBlock.nonce
                    )
                }

                2 -> {
                        calculateHash(
                        previousBlock.version!!,
                        previousBlock.index,
                        previousBlock.previousHash,
                        currentBlock.timestamp,
                        previousBlock.data.ticker,
                        previousBlock.data,
                        previousBlock.difficulty!!,
                        currentBlock.nonce
                    )
                }

                3 -> {
                    calculateHash(
                        previousBlock.version!!,
                        previousBlock.index,
                        previousBlock.previousHash,
                        currentBlock.timestamp,
                        previousBlock.data.ticker,
                        previousBlock.data,
                        previousBlock.difficulty!!,
                        currentBlock.nonce,
                        currentBlock.data.reward
                    )
                }

                else -> {
                    currentBlock.calculateHash()
                }
            }



            if (currentBlock.hash != calculatedHash) {
                return false
            }

            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }

    @Deprecated("Depracted from 1.0.5 use isValid() instead of", ReplaceWith("isValid()"))
    fun isValidChain(): Boolean {
        return isValid()
    }
}