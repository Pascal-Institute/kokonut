import kokonut.*
import kokonut.URL.FULL_NODE_0
import kokonut.Utility.Companion.isNodeHealthy
import kokonut.Utility.Companion.sendHttpPostRequest
import kokonut.core.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import java.io.File
import kotlin.math.min

fun main(): Unit = runBlocking{

    val blockChain = BlockChain()

    val wallet = Wallet(
        File("C:\\Users\\public\\private_key.pem"),
        File("C:\\Users\\public\\public_key.pem")
    )

    val miningURL = FULL_NODE_0

    if(blockChain.isValid() && wallet.isValid() && isNodeHealthy(miningURL)){

        val data =Data(
            0.0,
            Identity.ticker,
            miner= wallet.miner,
            null,
            "kokonut version 4")

        val newBlock : Block = blockChain.mine(FULL_NODE_0 ,data)
        val json = Json.encodeToJsonElement(newBlock)
        sendHttpPostRequest("${FULL_NODE_0}/addBlock", json, wallet.publicKeyFile)
    }
}