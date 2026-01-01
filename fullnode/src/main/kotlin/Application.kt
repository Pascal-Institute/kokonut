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
import kokonut.util.Router.Companion.handshake
import kokonut.util.Router.Companion.isValid
import kokonut.util.Router.Companion.root
import kokonut.util.Router.Companion.startValidating
import kokonut.util.Router.Companion.stopValidating
import kokonut.util.Utility


fun main() {
    BlockChain.initialize(NodeType.FULL)
    isValid()


    val host = System.getenv("SERVER_HOST") ?: "0.0.0.0"
    val port = 80

    // Start automatic heartbeat to Fuel Node
    val myAddress = "http://$host:$port"
    val fuelNodeAddress = BlockChain.knownPeer

    if (fuelNodeAddress != null) {
        println("🚀 Starting automatic registration to Fuel Node...")
        Utility.startHeartbeat(myAddress, fuelNodeAddress)
    } else {
        println("⚠️ No KOKONUT_PEER configured. Skipping automatic registration.")
        println("   Set KOKONUT_PEER environment variable to enable automatic registration.")
    }

    embeddedServer(Netty, host = host, port = port) {
                install(ContentNegotiation) { json() }

                routing {
                    root(NodeType.FULL)
                    handshake()
                    isValid()
                    getLastBlock()
                    getTotalCurrencyVolume()
                    getReward()
                    getChain()
                    startValidating()
                    addBlock()
                    stopValidating()
                }

            }
            .start(true)
}
