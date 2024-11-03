import kokonut.core.Block
import kokonut.util.Wallet
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.getLastBlock
import kokonut.core.BlockChain.Companion.getTotalCurrencyVolume
import kokonut.core.Version.genesisBlockID
import kokonut.core.Version.libraryVersion
import kokonut.core.Version.protocolVersion
import java.io.File

fun main() {
   val blockchain = BlockChain()

   println(libraryVersion)
   println(protocolVersion)
   println(genesisBlockID)
   println(getLastBlock())
   println(getTotalCurrencyVolume())

   val wallet = Wallet(
       File("C:\\Users\\public\\private_key.pem"),
       File("C:\\Users\\public\\public_key.pem"))
   println(wallet.isValid())
}