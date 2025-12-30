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
import kokonut.util.Router.Companion.root
import kokonut.util.Router.Companion.startMining
import kokonut.util.Router.Companion.stopMining

fun main() {
    val blockchain = BlockChain()
    // BlockChain.initialize() // Initialize manually when network is available
    isValid()

    val host = System.getenv("SERVER_HOST") ?: "0.0.0.0"
    embeddedServer(Netty, host = host, port = 80) {
                install(ContentNegotiation) { json() }

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
            }
            .start(true)
}
