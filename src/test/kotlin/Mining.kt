import kokonut.*
import kokonut.URLBook.FULL_NODE_0
import kokonut.util.API.Companion.isHealthy
import kokonut.core.*
import kokonut.util.API.Companion.addBlock
import kokonut.util.API.Companion.startMining
import kokonut.util.API.Companion.stopMining
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File

fun main(): Unit = runBlocking{

    val blockChain = BlockChain()

    val wallet = Wallet(
        File("C:\\Users\\public\\private_key.pem"),
        File("C:\\Users\\public\\public_key.pem")
    )

    val fullNode = FULL_NODE_0

    if(wallet.isValid() && fullNode.isHealthy()){

        if(!blockChain.isValid()){
            throw IllegalStateException("Chain is Invaild")
        }

        val data =Data(
            0.0,
            Identity.ticker,
            miner= wallet.miner,
            emptyList(),
            "Block Chain")

        try {
            val newBlock : Block = blockChain.mine(wallet, data)
            val json = Json.encodeToJsonElement(newBlock)
            fullNode.addBlock(json, wallet.publicKeyFile)
        }catch (e : Exception){
            fullNode.stopMining(wallet.publicKeyFile)
        }
    }
}