import kokonut.*
import kokonut.URLBook.FULL_NODE_0
import kokonut.util.API.Companion.isNodeHealthy
import kokonut.util.API.Companion.sendHttpPostRequest
import kokonut.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import java.net.URL

fun main(): Unit = runBlocking{

    val blockChain = BlockChain()

    val wallet = Wallet(
        File("C:\\Users\\public\\private_key.pem"),
        File("C:\\Users\\public\\public_key.pem")
    )

    val miningURL = FULL_NODE_0

    if(blockChain.isValid() && wallet.isValid() && miningURL.isNodeHealthy()){

        val data =Data(
            0.0,
            Identity.ticker,
            miner= wallet.miner,
            null,
            "welcome to kokonut")

        val newBlock : Block = blockChain.mine(miningURL ,data)
        val json = Json.encodeToJsonElement(newBlock)
        URL("${miningURL}/addBlock").sendHttpPostRequest(json, wallet.publicKeyFile)
    }
}