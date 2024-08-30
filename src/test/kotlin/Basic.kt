import kokonut.Wallet
import kokonut.core.BlockChain
import kokonut.core.Identity
import java.io.File

fun main() {
   val blockChain = BlockChain()

   println(Identity.version)

   println(blockChain.getLastBlock())

   println(blockChain.getTotalCurrencyVolume())

   val wallet = Wallet(
       File("C:\\Users\\public\\private_key.pem"),
       File("C:\\Users\\public\\public_key.pem"))
   println(wallet.isValid())
}