import kokonut.*
import kokonut.Utility.Companion.sendHttpPostRequest
import kokonut.Utility.Companion.sendHttpPostRequestWithFile
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

val blockChain = BlockChain()

fun main(): Unit = runBlocking{

    val job = launch {
        blockChain.loadChainFromNetwork()
    }

    job.join()
    val file = File("C:\\Users\\public\\public_key.pem")
    val address = Utility.calculateHash(Utility.loadPublicKey(file.path))
    val miner = Miner(address)
    val newBlock : Block = blockChain.mine(BlockData(miner.address, "Mining Kokonut"))
    var json = Json.encodeToJsonElement(newBlock)
    sendHttpPostRequestWithFile("http://kokonut.iptime.org/addBlock", json, file)
}