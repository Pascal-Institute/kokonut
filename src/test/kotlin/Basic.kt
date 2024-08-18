import kokonut.Miner
import kokonut.URL
import kokonut.Utility
import kokonut.block.Block
import kokonut.block.BlockChain
import kokonut.block.BlockData
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

fun main(): Unit = runBlocking{

    val blockChain = BlockChain()

    val job = launch {
        blockChain.loadChainFromNetwork()
    }

    job.join()

   //getLastBlock
   println(blockChain.getLastBlock())

   //getTotalCurrencyVolume
   println(blockChain.getTotalCurrencyVolume())

}