import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kokonut.core.BlockChain
import kokonut.core.WebSocketMessage
import kokonut.util.NodeType

/**
 * WebSocket client for LightNode to receive real-time block updates.
 * Eliminates the need for constant chain polling.
 */
class LightNodeWebSocketClient(
    private val state: AppState,
    private val peerAddress: String
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var client: HttpClient? = null
    private var isRunning = false
    
    companion object {
        const val RECONNECT_DELAY_MS = 5_000L
        const val PING_INTERVAL_MS = 30_000L
    }
    
    /**
     * Start WebSocket connection to FullNode.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        
        scope.launch {
            client = HttpClient(CIO) {
                install(io.ktor.client.plugins.websocket.WebSockets) {
                    pingIntervalMillis = PING_INTERVAL_MS
                }
            }
            
            while (isRunning) {
                try {
                    val wsUrl = peerAddress.replace("http://", "ws://").replace("https://", "wss://")
                    println("🔌 LightNode connecting to WebSocket: $wsUrl/ws/node")
                    state.webSocketStatus = "Connecting..."
                    
                    client?.webSocket("$wsUrl/ws/node") {
                        // Send registration
                        val registration = WebSocketMessage.NodeRegistration(
                            nodeAddress = "lightnode-${System.currentTimeMillis()}",
                            nodeType = NodeType.LIGHT.name,
                            chainSize = BlockChain.getChain().size.toLong()
                        )
                        send(Frame.Text(Json.encodeToString(registration)))
                        println("📤 LightNode registration sent")
                        
                        state.isWebSocketConnected = true
                        state.webSocketStatus = "Connected ✅"
                        
                        // Listen for incoming messages
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                handleMessage(text)
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("❌ WebSocket connection failed: ${e.message}")
                    state.isWebSocketConnected = false
                    state.webSocketStatus = "Disconnected (retrying...)"
                    delay(RECONNECT_DELAY_MS)
                }
            }
        }
    }
    
    /**
     * Handle incoming WebSocket messages.
     */
    private fun handleMessage(text: String) {
        try {
            val message = Json.decodeFromString<WebSocketMessage>(text)
            
            when (message) {
                is WebSocketMessage.RegistrationAck -> {
                    println("✅ LightNode registered: ${message.message}")
                    state.webSocketStatus = "Connected (${message.registeredNodes} nodes)"
                }
                
                is WebSocketMessage.NewBlock -> {
                    println("📥 Received new block #${message.block.index}")
                    
                    // Validate and add block to local chain
                    val lastBlock = BlockChain.getLastBlock()
                    val block = message.block
                    
                    if (block.index == lastBlock.index + 1 && 
                        block.previousHash == lastBlock.hash && 
                        block.isValid()) {
                        BlockChain.database.insert(block)
                        BlockChain.refreshFromDatabase()
                        
                        // Update UI with new chain size
                        state.localChainSize = BlockChain.getChain().size
                        
                        // Check if this block contains a reward for this validator
                        val rewardTx = block.data.transactions.find {
                            it.transaction == "VALIDATOR_REWARD" && 
                            it.receiver == state.validatorAddress
                        }
                        
                        if (rewardTx != null) {
                            // Update balance locally (avoid HTTP call)
                            val currentBalance = BlockChain.getBalance(state.validatorAddress)
                            state.walletBalance = currentBalance
                            println("💰 Validator reward received: ${rewardTx.remittance} KNT")
                        }
                    }
                }
                
                is WebSocketMessage.ChainSyncResponse -> {
                    println("📥 Chain sync: ${message.blocks.size} blocks")
                    message.blocks.forEach { block ->
                        if (block.isValid()) {
                            BlockChain.database.insert(block)
                        }
                    }
                    BlockChain.refreshFromDatabase()
                    state.localChainSize = BlockChain.getChain().size
                }
                
                is WebSocketMessage.Pong -> {
                    // Keep-alive acknowledged
                }
                
                else -> {
                    println("⚠️ Unhandled message: ${message::class.simpleName}")
                }
            }
        } catch (e: Exception) {
            println("❌ Failed to handle message: ${e.message}")
        }
    }
    
    /**
     * Request chain sync from FullNode.
     */
    suspend fun requestChainSync(fromIndex: Long) {
        val request = WebSocketMessage.ChainSyncRequest(
            fromIndex = fromIndex,
            requestingNode = "lightnode-${System.currentTimeMillis()}"
        )
        
        // This would need to be sent through the active WebSocket connection
        // For now, we'll rely on automatic sync on connection
        println("📤 Requesting chain sync from index $fromIndex")
    }
    
    /**
     * Stop WebSocket connection.
     */
    fun stop() {
        isRunning = false
        scope.cancel()
        client?.close()
        state.isWebSocketConnected = false
        state.webSocketStatus = "Disconnected"
        println("🔌 LightNode WebSocket disconnected")
    }
}
