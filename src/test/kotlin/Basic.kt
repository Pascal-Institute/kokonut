import kokonut.Wallet
import kokonut.block.BlockChain
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

   val wallet = Wallet(
       File("C:\\Users\\public\\private_key.pem"),
       File("C:\\Users\\public\\public_key.pem"))
   println(wallet.isValid())
}