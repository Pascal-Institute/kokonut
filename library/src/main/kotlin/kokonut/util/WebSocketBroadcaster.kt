package kokonut.util

import kokonut.core.Block

/**
 * Interface for broadcasting blocks via WebSocket.
 * Implementation is provided by FuelNode.
 */
interface WebSocketBroadcaster {
    suspend fun broadcastBlock(block: Block, sourceAddress: String)
}

/**
 * Global WebSocket broadcaster instance.
 * Set by FuelNode on initialization.
 */
object WebSocketBroadcasterRegistry {
    private var broadcaster: WebSocketBroadcaster? = null
    
    fun register(broadcaster: WebSocketBroadcaster) {
        this.broadcaster = broadcaster
    }
    
    suspend fun broadcast(block: Block, sourceAddress: String) {
        broadcaster?.broadcastBlock(block, sourceAddress)
            ?: println("⚠️ WebSocket broadcaster not available (HTTP fallback mode)")
    }
    
    fun isAvailable(): Boolean = broadcaster != null
}
