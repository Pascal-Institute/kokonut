import kokonut.util.Utility.Companion.calculateHash
import kokonut.core.Block
import kokonut.core.Data
import kokonut.core.Identity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

fun main() {

    val version = 4
    val totalReward = 0.0
    val hex = "123456789abcdef"
    val ticker = Identity.ticker
    val difficulty = 48
    val miner =  "0".repeat(64)

    val comment = "Navigate beyond computing oceans"
    val timeStamp = System.currentTimeMillis()
    val artificialLeadingZeros = "0".repeat(difficulty)
    val hash = artificialLeadingZeros + hex[(timeStamp % 10).toInt()] + calculateHash(timeStamp).substring(difficulty + 1, miner.length)


    val data = Data(
        reward = totalReward,
        ticker = ticker,
        miner = miner,
        transactions = listOf(),
        comment = comment
    )

    val genesisBlock = Block(
        version = version,
        index = 0,
        previousHash = "0",
        timestamp = timeStamp,
        data = data,
        difficulty = 0,
        nonce = 0,
        hash = hash,
    )


    val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    val jsonString = json.encodeToString(genesisBlock)

    // JSON 문자열을 파일로 저장
    val file = File("src\\main\\resources\\${hash}.json")
    file.writeText(jsonString)

    println("Genesis Block is Generated")
}