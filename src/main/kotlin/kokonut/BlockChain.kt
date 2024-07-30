package kokonut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.http.cio.*
import io.ktor.serialization.kotlinx.json.*
import kokonut.Block.Companion.calculateHash
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.max

class BlockChain {

    val chain: MutableList<Block>
    private val difficultyAdjustInterval = 2024
    private val targetBlockTime = 10 * 60 * 1000L
    private val version = 1
    private val ticker = "KNT"
    private val minimumDifficulty = 4

    init {
        chain = mutableListOf()
    }

    suspend fun loadChainFromNetwork() = runBlocking {
        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val repoUrl = "https://api.github.com/repos/Pascal-Institute/kokonut-storage/contents/"

        try {
            // 저장소 파일 목록 요청
            val files: List<GitHubFile> = client.get(repoUrl).body()

            // JSON 파일 URL 리스트를 생성
            val jsonUrls = files.filter { it.type == "file" && it.name.endsWith(".json") }
                .map { "https://raw.githubusercontent.com/Pascal-Institute/kokonut-storage/main/${it.path}" }

            // JSON 파일 읽기 및 처리
            for (url in jsonUrls) {
                val response: HttpResponse = client.get(url)
                val responseBody = response.bodyAsText()
                try {
                    val block : Block = Json.decodeFromString(responseBody)
                    val updatedBlock = block.copy(
                        version = block.version ?: 1,
                        difficulty = block.difficulty ?: 8  // city가 null인 경우 기본값 "Unknown" 설정
                    )

                    chain.add(updatedBlock)
                } catch (e: Exception) {
                    println("JSON Passer Error: ${e.message}")
                }
            }

        } catch (e: Exception) {
            println("Error! : ${e.message}")
        } finally {
            client.close()
        }
    }

    fun getLastBlock(): Block {
        return chain.last()
    }

    fun mine() : String {
        var nonce : Long = 0
        var timestamp = System.currentTimeMillis()
        var miningHash = getLastBlock().calculateHash(timestamp, nonce)
        while(getLastBlock().difficulty!! > countLeadingZeros(miningHash)){
            timestamp = System.currentTimeMillis()
            nonce++
            miningHash = getLastBlock().calculateHash(timestamp, nonce)
            println("Minining Hash : $miningHash")
            println("Nonce : $nonce")
        }

        return miningHash
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

        //Send Http Post
        chain.add(block)
        return true
    }

    fun calculateTargetDifficulty(currentIndex: Int): Int {
        val lastAdjustmentBlock = chain.getOrNull(currentIndex - difficultyAdjustInterval) ?: return 8
        val timeTaken = System.currentTimeMillis() - lastAdjustmentBlock.timestamp
        val difficultyFactor = timeTaken / targetBlockTime
        val targetDifficulty = max(minimumDifficulty, 16 - difficultyFactor.toInt())
        return targetDifficulty
    }

    fun isValidChain(): Boolean {
        for (i in 1 until chain.size) {
            val currentBlock = chain[i]
            val previousBlock = chain[i - 1]

            if (currentBlock.hash != calculateHash(currentBlock.version!!, currentBlock.index, currentBlock.previousHash, currentBlock.timestamp, currentBlock.ticker, currentBlock.data, currentBlock.difficulty!!, currentBlock.nonce)) {
                return false
            }

            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }
}