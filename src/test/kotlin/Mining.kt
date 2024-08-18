import kokonut.*
import kokonut.URL.FULL_NODE_0
import kokonut.URL.FULL_NODE_1
import kokonut.Utility.Companion.isNodeHealthy
import kokonut.Utility.Companion.sendHttpPostRequest
import kokonut.block.Block
import kokonut.block.BlockChain
import kokonut.block.BlockData
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

    if(isNodeHealthy(FULL_NODE_0)){
        val newBlock : Block = blockChain.mine(BlockData(miner.address, "Finale FIN?"))
        val json = Json.encodeToJsonElement(newBlock)
        sendHttpPostRequest("${FULL_NODE_0}/addBlock", json, file)
    }
}