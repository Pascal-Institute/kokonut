import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kokonut.core.BlockChain
import kokonut.core.BlockChain.Companion.isValid
import kokonut.util.NodeType
import kokonut.util.Router.Companion.addBlock
import kokonut.util.Router.Companion.getChain
import kokonut.util.Router.Companion.getLastBlock
import kokonut.util.Router.Companion.getReward
import kokonut.util.Router.Companion.getTotalCurrencyVolume
import kokonut.util.Router.Companion.isValid
import kokonut.util.Router.Companion.register
import kokonut.util.Router.Companion.startMining
import kokonut.util.Router.Companion.stopMining
import kokonut.util.Router.Companion.submit
import kokonut.util.Router.Companion.root

fun main() {
    val blockchain = BlockChain()
    isValid()

    embeddedServer(Netty, host = "0.0.0.0", port = 8080) {
        install(ContentNegotiation) {
            json()
        }

        routing {
            root(NodeType.FULL)
            register()
            isValid()
            getLastBlock()
            getTotalCurrencyVolume()
            getReward()
            getChain()
            startMining()
            addBlock()
            stopMining()
        }
    }.start(true)
}