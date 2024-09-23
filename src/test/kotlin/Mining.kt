import kokonut.util.API.Companion.isHealthy
import kokonut.core.*
import kokonut.core.BlockChain.TICKER
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

    val fullNode = URL(BlockChain.getLongestChainFullNode().ServiceAddress)

    println(fullNode)

    if(wallet.isValid() && fullNode.isHealthy()){

        if(!BlockChain.isValid()){
            throw IllegalStateException("Chain is Invaild")
        }

        val data =Data(
            0.0,
            TICKER,
            miner= wallet.miner,
            emptyList(),
            "Block Chain")

        try {
            fullNode.startMining(wallet.publicKeyFile)
            val newBlock : Block = BlockChain.mine(wallet, data)
            val json = Json.encodeToJsonElement(newBlock)
            fullNode.addBlock(json, wallet.publicKeyFile)
            BlockChain.miningState = MiningState.MINED
        }catch (e : Exception){
            BlockChain.miningState = MiningState.FAILED
            fullNode.stopMining(wallet.publicKeyFile)
        }
    }

    BlockChain.miningState = MiningState.READY
}