import kokonut.Block
import kokonut.BlockChain
import kokonut.BlockData
import kokonut.Utility.Companion.sendHttpPostRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

val blockChain = BlockChain()

fun main(): Unit = runBlocking{

    val job = launch {
        blockChain.loadChainFromNetwork()
    }

    job.join()

    val newBlock : Block = blockChain.mine(BlockData("Anonymous Miner", "The world will be sustainable society"))

    var json = Json.encodeToJsonElement(newBlock)
    sendHttpPostRequest("http://kokonut.iptime.org:2030/addBlock", json)

}