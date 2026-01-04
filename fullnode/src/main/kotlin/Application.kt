import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.websocket.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kokonut.core.BlockChain
import kokonut.core.WebSocketMessage
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

/** WebSocket client configuration */
private object WSClientConfig {
    const val RECONNECT_DELAY_MS = 5_000L
    const val PING_INTERVAL_MS = 30_000L
}

/**
 * Connects to FuelNode via WebSocket and maintains persistent connection.
 */
@OptIn(DelicateCoroutinesApi::class)
private fun connectToFuelNodeWebSocket(myAddress: String, fuelNodeAddress: String) {
    GlobalScope.launch {
        val client = HttpClient(CIO) {
            install(io.ktor.client.plugins.websocket.WebSockets) {
                pingIntervalMillis = WSClientConfig.PING_INTERVAL_MS
            }
        }
        
        while (isActive) {
            try {
                val wsUrl = fuelNodeAddress.replace("http://", "ws://").replace("https://", "wss://")
                println("🔌 Connecting to FuelNode WebSocket: $wsUrl/ws/node")
                
                client.webSocket("$wsUrl/ws/node") {
                    // Send registration message
                    val registration = WebSocketMessage.NodeRegistration(
                        nodeAddress = myAddress,
                        nodeType = NodeType.FULL.name,
                        chainSize = BlockChain.getChain().size.toLong()
                    )
                    send(Frame.Text(Json.encodeToString(registration)))
                    println("📤 Sent registration to FuelNode")
                    
                    // Listen for incoming messages
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            val message = Json.decodeFromString<WebSocketMessage>(text)
                            
                            when (message) {
                                is WebSocketMessage.RegistrationAck -> {
                                    println("✅ Registration acknowledged: ${message.message} (${message.registeredNodes} nodes)")
                                }
                                
                                is WebSocketMessage.NewBlock -> {
                                    println("📥 Received new block #${message.block.index} from ${message.sourceNodeAddress}")
                                    
                                    // Validate and add block to database
                                    val lastBlock = BlockChain.getLastBlock()
                                    val block = message.block
                                    
                                    // Basic validation
                                    if (block.index == lastBlock.index + 1 && 
                                        block.previousHash == lastBlock.hash && 
                                        block.isValid()) {
                                        BlockChain.database.insert(block)
                                        BlockChain.refreshFromDatabase()
                                        println("✅ Block #${block.index} added successfully")
                                    } else {
                                        println("❌ Block #${block.index} validation failed")
                                    }
                                }
                                
                                is WebSocketMessage.ChainSyncResponse -> {
                                    println("📥 Received chain sync: ${message.blocks.size} blocks")
                                    
                                    // Add received blocks
                                    message.blocks.forEach { block ->
                                        if (block.isValid()) {
                                            BlockChain.database.insert(block)
                                        }
                                    }
                                    BlockChain.refreshFromDatabase()
                                    println("✅ Chain synchronized - Total blocks: ${BlockChain.getChain().size}")
                                }
                                
                                is WebSocketMessage.Pong -> {
                                    // Keep-alive response
                                }
                                
                                else -> {
                                    println("⚠️ Unhandled WebSocket message: ${message::class.simpleName}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ WebSocket connection failed: ${e.message}")
                println("🔄 Retrying in ${WSClientConfig.RECONNECT_DELAY_MS / 1000} seconds...")
                delay(WSClientConfig.RECONNECT_DELAY_MS)
            }
        }
    }
}

fun main() {
    BlockChain.initialize(NodeType.FULL)
    BlockChain.isValid()

    val host = System.getenv("SERVER_HOST") ?: ServerConfig.DEFAULT_HOST
    val port = ServerConfig.DEFAULT_PORT

    // Start automatic heartbeat to Fuel Node
    val myAddress = Utility.getAdvertiseAddress(host, port)
    val fuelNodeAddress = BlockChain.knownPeer

    // Connect via WebSocket for real-time updates
    startWebSocketConnectionIfConfigured(myAddress, fuelNodeAddress)

    embeddedServer(Netty, host = host, port = port) {
        configureServer()
    }.start(wait = true)
}

/**
 * Starts WebSocket connection to FuelNode if configured.
 */
private fun startWebSocketConnectionIfConfigured(myAddress: String, fuelNodeAddress: String?) {
    if (fuelNodeAddress != null) {
        println("🚀 Starting WebSocket connection to Fuel Node...")
        connectToFuelNodeWebSocket(myAddress, fuelNodeAddress)
    } else {
        println("⚠️ No KOKONUT_PEER configured. Skipping WebSocket connection.")
        println("   Set KOKONUT_PEER environment variable to enable real-time updates.")
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
