import kokonut.util.API.Companion.isHealthy
import kokonut.core.*
import kokonut.state.MiningState
import kokonut.util.API.Companion.addBlock
import kokonut.util.API.Companion.stopMining
import kokonut.util.Utility.Companion.getLongestChainFullNode
import kokonut.util.Wallet
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

    val fullNode = URL(getLongestChainFullNode().ServiceAddress)

    println(fullNode)

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
            blockChain.miningState = MiningState.MINED
        }catch (e : Exception){
            blockChain.miningState = MiningState.FAILED
            fullNode.stopMining(wallet.publicKeyFile)
        }
    }

    blockChain.miningState = MiningState.READY
}