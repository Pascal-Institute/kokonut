package kokonut.block

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.GitHubFile
import kokonut.Identity.name
import kokonut.Identity.ticker
import kokonut.Policy
import kokonut.URL.FUEL_NODE
import kokonut.URL.FULL_RAW_STORAGE
import kokonut.URL.FULL_STORAGE
import kokonut.block.Block.Companion.calculateHash
import kokonut.Utility.Companion.sendHttpGetPolicy
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class BlockChain {

    private val chain: MutableList<Block> = mutableListOf()
    private val genesisBlockDifficulty = 32
    private val genesisVersion = 0

    init {
        //MUST TO DO : Injection Identity
        name = "Kokonut"
        ticker = "KNT"
    }

    suspend fun loadChainFromNetwork() = runBlocking {

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
                val responseBody = response.bodyAsText()
                try {
                    val block : Block = Json.decodeFromString(responseBody)
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

            sortChainByIndex()

        } catch (e: Exception) {
            println("Error! : ${e.message}")
        } finally {
            client.close()
        }
    }

    fun sortChainByIndex() {
        chain.sortBy { it.index }
    }

    fun getLastBlock(): Block {
        return chain.last()
    }


    fun mine() : Block {
       return mine(BlockData("", "Dummy"))
    }

    fun mine(blockData: BlockData) : Block {

        val policy = sendHttpGetPolicy(FUEL_NODE)
        var nonce : Long = 0
        var timestamp = System.currentTimeMillis()
        var miningHash = getLastBlock().calculateHash(timestamp, nonce, policy.reward)

        while(policy.difficulty > countLeadingZeros(miningHash)){
            timestamp = System.currentTimeMillis()
            nonce++
            miningHash = getLastBlock().calculateHash(timestamp, nonce, policy.reward)
            println("Nonce : $nonce")
        }

        return Block(policy.version,getLastBlock().index + 1, getLastBlock().hash, timestamp, ticker,
            blockData, policy.difficulty, nonce, miningHash, policy.reward)
    }

    private fun countLeadingZeros(hash: String): Int {
        return hash.takeWhile { it == '0' }.length
    }

    fun addBlock(policy: Policy, block: Block) : Boolean{

        //Proven Of Work
        if(block.version != policy.version){
            return false
        }

        if(block.index != getLastBlock().index + 1){
            return false
        }

        if(block.previousHash != getLastBlock().hash){
            return false
        }

        if(block.ticker != ticker){
            return false
        }

        val calculatedHash =  calculateHash(block.version, block.index, block.previousHash, block.timestamp, block.ticker, block.data, block.difficulty!!, block.nonce, block.reward!!)
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
                        previousBlock.ticker,
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
                        previousBlock.ticker,
                        previousBlock.data,
                        previousBlock.difficulty!!,
                        currentBlock.nonce
                    )
                }

                else -> {
                    calculateHash(
                        previousBlock.version!!,
                        previousBlock.index,
                        previousBlock.previousHash,
                        currentBlock.timestamp,
                        previousBlock.ticker,
                        previousBlock.data,
                        previousBlock.difficulty!!,
                        currentBlock.nonce,
                        currentBlock.reward!!
                    )
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