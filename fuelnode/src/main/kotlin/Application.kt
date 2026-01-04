import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kokonut.core.Block
import kokonut.core.BlockChain
import kokonut.core.WebSocketMessage
import kokonut.util.NodeType
import kokonut.util.WebSocketBroadcaster
import kokonut.util.WebSocketBroadcasterRegistry
import kokonut.util.Router.Companion.getChain
import kokonut.util.Router.Companion.getFullNodes
import kokonut.util.Router.Companion.getFullNodesFromHeartbeat
import kokonut.util.Router.Companion.getGenesisBlock
import kokonut.util.Router.Companion.getPolicy
import kokonut.util.Router.Companion.heartbeat
import kokonut.util.Router.Companion.propagate
import kokonut.util.Router.Companion.root

/** Constants for node health management */
private object NodeHealthConfig {
    const val STALE_NODE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
    const val CLEANUP_INTERVAL_MS = 60_000L // 1 minute
    const val CLEANUP_INITIAL_DELAY_MS = 60_000L // 1 minute
    const val DEFAULT_SERVER_HOST = "0.0.0.0"
    const val DEFAULT_SERVER_PORT = 80
}

/** Constants for WebSocket configuration */
private object WebSocketConfig {
    const val PING_INTERVAL_MS = 30_000L // 30 seconds
    const val TIMEOUT_MS = 60_000L // 1 minute
}

/** Connected WebSocket sessions: nodeAddress -> WebSocketServerSession */
private val connectedNodes = ConcurrentHashMap<String, DefaultWebSocketServerSession>()

/** Broadcast a message to all connected WebSocket nodes */
suspend fun broadcastToNodes(message: WebSocketMessage) {
    val messageText = Json.encodeToString(message)
    connectedNodes.values.forEach { session ->
        try {
            session.send(Frame.Text(messageText))
        } catch (e: Exception) {
            println("❌ Failed to send message to session: ${e.message}")
        }
    }
}

/** Broadcast new block to all nodes */
suspend fun broadcastBlock(block: Block, sourceAddress: String) {
    val message = WebSocketMessage.NewBlock(block, sourceAddress)
    broadcastToNodes(message)
    println("📡 Broadcasting block #${block.index} to ${connectedNodes.size} connected nodes")
}

fun main() {
    BlockChain.initialize(NodeType.FUEL)

    // Register WebSocket broadcaster
    WebSocketBroadcasterRegistry.register(object : WebSocketBroadcaster {
        override suspend fun broadcastBlock(block: Block, sourceAddress: String) {
            broadcastBlock(block, sourceAddress)
        }
    })

    // ConcurrentHashMap: address -> lastHeartbeatTimestamp
    val fullNodes = ConcurrentHashMap<String, Long>()

    // Cleanup timer: Remove nodes that haven't sent heartbeat within timeout period
    Timer("FullNode-Cleanup", true)
            .scheduleAtFixedRate(
                    object : TimerTask() {
                        override fun run() {
                            val now = System.currentTimeMillis()
                            fullNodes.entries.removeIf { (address, lastSeen) ->
                                val isStale = (now - lastSeen) > NodeHealthConfig.STALE_NODE_TIMEOUT_MS
                                if (isStale) {
                                    println("🗑️ Removing stale Full Node: $address (inactive)")
                                }
                                isStale
                            }
                        }
                    },
                    NodeHealthConfig.CLEANUP_INITIAL_DELAY_MS,
                    NodeHealthConfig.CLEANUP_INTERVAL_MS
            )

    val host = System.getenv("SERVER_HOST") ?: NodeHealthConfig.DEFAULT_SERVER_HOST
    embeddedServer(Netty, host = host, port = NodeHealthConfig.DEFAULT_SERVER_PORT) {
                install(ContentNegotiation) { json() }
                install(WebSockets) {
                    pingPeriodMillis = WebSocketConfig.PING_INTERVAL_MS
                    timeoutMillis = WebSocketConfig.TIMEOUT_MS
                    maxFrameSize = Long.MAX_VALUE
                    masking = false
                }
                routing {
                    root(NodeType.FUEL)
                    
                    // WebSocket endpoint for real-time communication
                    webSocket("/ws/node") {
                        var nodeAddress: String? = null
                        
                        try {
                            println("🔌 New WebSocket connection from ${call.request.local.remoteHost}")
                            
                            for (frame in incoming) {
                                if (frame is Frame.Text) {
                                    val text = frame.readText()
                                    val message = Json.decodeFromString<WebSocketMessage>(text)
                                    
                                    when (message) {
                                        is WebSocketMessage.NodeRegistration -> {
                                            nodeAddress = message.nodeAddress
                                            connectedNodes[nodeAddress] = this
                                            fullNodes[nodeAddress] = message.timestamp
                                            
                                            val ack = WebSocketMessage.RegistrationAck(
                                                success = true,
                                                message = "Node registered successfully",
                                                registeredNodes = connectedNodes.size
                                            )
                                            send(Frame.Text(Json.encodeToString(ack)))
                                            println("✅ Registered node: $nodeAddress (${message.nodeType}) - Total: ${connectedNodes.size}")
                                        }
                                        
                                        is WebSocketMessage.NewBlock -> {
                                            // Broadcast block to other nodes
                                            broadcastBlock(message.block, message.sourceNodeAddress)
                                        }
                                        
                                        is WebSocketMessage.ChainSyncRequest -> {
                                            val blocks = BlockChain.getChain()
                                                .drop(message.fromIndex.toInt())
                                            
                                            val response = WebSocketMessage.ChainSyncResponse(
                                                blocks = blocks,
                                                totalChainSize = BlockChain.getChain().size.toLong()
                                            )
                                            send(Frame.Text(Json.encodeToString(response)))
                                            println("📤 Sent ${blocks.size} blocks to ${message.requestingNode}")
                                        }
                                        
                                        is WebSocketMessage.Ping -> {
                                            val pong = WebSocketMessage.Pong(System.currentTimeMillis())
                                            send(Frame.Text(Json.encodeToString(pong)))
                                        }
                                        
                                        else -> {
                                            println("⚠️ Unhandled message type: ${message::class.simpleName}")
                                        }
                                    }
                                }
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            println("🔌 WebSocket connection closed: ${e.message}")
                        } catch (e: Exception) {
                            println("❌ WebSocket error: ${e.message}")
                        } finally {
                            nodeAddress?.let { address ->
                                connectedNodes.remove(address)
                                fullNodes.remove(address)
                                println("🔌 Disconnected node: $address - Remaining: ${connectedNodes.size}")
                            }
                        }
                    }
                    
                    // HTTP endpoints (Query-only)
                    heartbeat(fullNodes)
                    getFullNodesFromHeartbeat(fullNodes)
                    getFullNodes(fullNodes)
                    getGenesisBlock()
                    getPolicy()
                    getChain()
                    propagate()
                }
            }
            .start(true)
}
