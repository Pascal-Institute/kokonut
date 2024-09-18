import kokonut.util.Wallet
import kokonut.core.BlockChain
import kokonut.core.Version.libraryVersion
import kokonut.core.Version.protocolVersion
import java.io.File

fun main() {

   println(libraryVersion)

   println(protocolVersion)

   val blockChain = BlockChain()
   println(blockChain.FULL_NODE)
   println(blockChain.getLastBlock())
   println(blockChain.getTotalCurrencyVolume())

   val wallet = Wallet(
       File("C:\\Users\\public\\private_key.pem"),
       File("C:\\Users\\public\\public_key.pem"))
   println(wallet.isValid())
}