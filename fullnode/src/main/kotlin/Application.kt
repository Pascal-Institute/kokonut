import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kokonut.core.BlockChain
import kokonut.util.NodeType
import kokonut.util.Router.Companion.addBlock
import kokonut.util.Router.Companion.getBalance
import kokonut.util.Router.Companion.getChain
import kokonut.util.Router.Companion.getGenesisBlock
import kokonut.util.Router.Companion.getKnownFullNodes
import kokonut.util.Router.Companion.getLastBlock
import kokonut.util.Router.Companion.getReward
import kokonut.util.Router.Companion.getTotalCurrencyVolume
import kokonut.util.Router.Companion.getValidators
import kokonut.util.Router.Companion.handshake
import kokonut.util.Router.Companion.isValid
import kokonut.util.Router.Companion.root
import kokonut.util.Router.Companion.stakeLock
import kokonut.util.Router.Companion.startValidating
import kokonut.util.Router.Companion.stopValidating
import kokonut.util.Router.Companion.transactionsDashboard
import kokonut.util.Utility

/** Server configuration constants */
private object ServerConfig {
    const val DEFAULT_HOST = "0.0.0.0"
    const val DEFAULT_PORT = 80
}

fun main() {
    BlockChain.initialize(NodeType.FULL)
    BlockChain.isValid()

    val host = System.getenv("SERVER_HOST") ?: ServerConfig.DEFAULT_HOST
    val port = ServerConfig.DEFAULT_PORT

    // Start automatic heartbeat to Fuel Node
    val myAddress = Utility.getAdvertiseAddress(host, port)
    val fuelNodeAddress = BlockChain.knownPeer

    startHeartbeatIfConfigured(myAddress, fuelNodeAddress)

    embeddedServer(Netty, host = host, port = port) {
        configureServer()
    }.start(wait = true)
}

/**
 * Starts heartbeat service if Fuel Node peer is configured.
 */
private fun startHeartbeatIfConfigured(myAddress: String, fuelNodeAddress: String?) {
    if (fuelNodeAddress != null) {
        println("🚀 Starting automatic registration to Fuel Node...")
        Utility.startHeartbeat(myAddress, fuelNodeAddress)
    } else {
        println("⚠️ No KOKONUT_PEER configured. Skipping automatic registration.")
        println("   Set KOKONUT_PEER environment variable to enable automatic registration.")
    }
}

/**
 * Configures the Ktor server with content negotiation and routing.
 */
private fun Application.configureServer() {
    install(ContentNegotiation) { json() }

    routing {
        root(NodeType.FULL)
        handshake()
        isValid()
        getGenesisBlock()
        getLastBlock()
        getBalance()
        getTotalCurrencyVolume()
        getReward()
        getChain()
        transactionsDashboard()
        getValidators()
        stakeLock()
        startValidating()
        addBlock()
        stopValidating()
        getKnownFullNodes()
    }
}
