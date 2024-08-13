import kokonut.Block
import kokonut.BlockChain
import kokonut.BlockData
import kokonut.Miner
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

    val miner = Miner("6c60b7550766d5ae24ccc3327f0e47fbaa51e599172795bb9ad06ac82784a92d")

    val newBlock : Block = blockChain.mine(BlockData(miner.address, "Mining Kokonut"))

    var json = Json.encodeToJsonElement(newBlock)
    sendHttpPostRequest("http://kokonut.iptime.org:2030/addBlock", json)

}