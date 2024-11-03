import kokonut.util.API.Companion.isHealthy
import kokonut.core.*
import kokonut.core.BlockChain.Companion.TICKER
import kokonut.core.BlockChain.Companion.fullNode
import kokonut.state.MiningState
import kokonut.util.API.Companion.addBlock
import kokonut.util.API.Companion.startMining
import kokonut.util.API.Companion.stopMining
import kokonut.util.Wallet
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.net.URL

fun main(): Unit = runBlocking{

    val wallet = Wallet(
        File("C:\\Users\\public\\private_key.pem"),
        File("C:\\Users\\public\\public_key.pem")
    )

    if(wallet.isValid() && URL(fullNode.address).isHealthy()){

        if(!BlockChain.isValid()){
            throw IllegalStateException("Chain is Invaild")
        }

        val data =Data(
            0.0,
            TICKER,
            miner= wallet.miner,
            emptyList(),
            "Propagate Vision of Pascal Institute")

        try {
            URL(fullNode.address).startMining(wallet.publicKeyFile)
            val newBlock : Block = BlockChain.mine(wallet, data)
            val json = Json.encodeToJsonElement(newBlock)

            //propagate...
            BlockChain.fullNodes.forEach {

                    try {
                        URL(it.address).addBlock(json, wallet.publicKeyFile)
                    } catch (e: Exception) {
                        println("Propagation Failed at ${it.address} : $e")
                    }

            }
            wallet.miningState = MiningState.MINED
        }catch (e : Exception){
            wallet.miningState = MiningState.FAILED
            URL(fullNode.address).stopMining(wallet.publicKeyFile)
        }
    }

    wallet.miningState = MiningState.READY
}