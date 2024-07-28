package kokonut

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

class BlockChain {

    val chain: MutableList<Block>
    private val ticker = "KNT"

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
                    chain.add(block)
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

    fun addBlock(data: BlockData) {
        val previousBlock = getLastBlock()
        val newBlock = Block(
            previousBlock.index + 1,
            previousBlock.hash,
            System.currentTimeMillis(),
            ticker,
            data,
           0.0,
            Block.calculateHash(previousBlock.index + 1, previousBlock.hash, System.currentTimeMillis(), ticker, previousBlock.nonce, data)
        )

        chain.add(newBlock)
    }

    fun isValidChain(): Boolean {
        for (i in 1 until chain.size) {
            val currentBlock = chain[i]
            val previousBlock = chain[i - 1]

            if (currentBlock.hash != Block.calculateHash(currentBlock.index, currentBlock.previousHash, currentBlock.timestamp, currentBlock.ticker, currentBlock.nonce, currentBlock.data)) {
                return false
            }

            if (currentBlock.previousHash != previousBlock.hash) {
                return false
            }
        }
        return true
    }
}