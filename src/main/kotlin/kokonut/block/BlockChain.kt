package kokonut.block

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.GitHubFile
import kokonut.URL.FULL_RAW_STORAGE
import kokonut.URL.FULL_STORAGE
import kokonut.block.Block.Companion.calculateHash
import kokonut.Utility.Companion.difficulty
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.math.max

class BlockChain {

    val chain: MutableList<Block>
    private val difficultyAdjustInterval = 2024
    private val targetBlockTime = 10 * 60 * 1000L
    private val version = 1
    private val ticker = "KNT"
    private val minimumDifficulty = 4
    private val genesisBlockDifficulty = 32
    private val genesisVersion = 0

    init {
        chain = mutableListOf()
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

            // JSON 파일 읽기 및 처리
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
        var nonce : Long = 0
        var timestamp = System.currentTimeMillis()
        var miningHash = getLastBlock().calculateHash(timestamp, nonce)
        while(difficulty > countLeadingZeros(miningHash)){
            timestamp = System.currentTimeMillis()
            nonce++
            miningHash = getLastBlock().calculateHash(timestamp, nonce)
            println("Nonce : $nonce")
        }

        return Block(version,getLastBlock().index + 1, getLastBlock().hash, timestamp, ticker,
            blockData, difficulty, nonce, miningHash)
    }

    fun countLeadingZeros(hash: String): Int {
        return hash.takeWhile { it == '0' }.length
    }

    fun addBlock(block: Block) : Boolean{

        //Proven Of Work
        if(block.version != version){
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

        val calculatedHash =  calculateHash(block.version, block.index, block.previousHash, block.timestamp, block.ticker, block.data, block.difficulty!!, block.nonce)
        if(block.hash != calculatedHash){
            return false
        }

        //Proven done!
        chain.add(block)
        return true
    }

    fun calculateTargetDifficulty(currentIndex: Int): Int {
        val lastAdjustmentBlock = chain.getOrNull(currentIndex - difficultyAdjustInterval) ?: return 8
        val timeTaken = System.currentTimeMillis() - lastAdjustmentBlock.timestamp
        val difficultyFactor = timeTaken / targetBlockTime
        val targetDifficulty = max(minimumDifficulty, 8 - difficultyFactor.toInt())
        return targetDifficulty
    }

    fun isValid(): Boolean {
        for (i in chain.size - 1 downTo  1) {
            val currentBlock = chain[i]
            val previousBlock = chain[i - 1]
            val calculatedHash = Block.calculateHash(
                previousBlock.version!!,
                previousBlock.index,
                previousBlock.previousHash,
                currentBlock.timestamp,
                previousBlock.ticker,
                previousBlock.data,
                previousBlock.difficulty!!,
                currentBlock.nonce
            )

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