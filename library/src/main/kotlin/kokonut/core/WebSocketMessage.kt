package kokonut.core

import kotlinx.serialization.Serializable

/**
 * WebSocket message types for real-time blockchain communication.
 * 
 * Replaces HTTP polling with push-based notifications for:
 * - Block propagation
 * - Network updates
 * - Node registration
 */
@Serializable
sealed class WebSocketMessage {
    
    /**
     * New block notification - broadcasted to all connected nodes
     */
    @Serializable
    data class NewBlock(
        val block: Block,
        val sourceNodeAddress: String
    ) : WebSocketMessage()
    
    /**
     * Network rules update notification
     */
    @Serializable
    data class NetworkUpdate(
        val rules: NetworkRules
    ) : WebSocketMessage()
    
    /**
     * Node registration (replaces HTTP /heartbeat)
     */
    @Serializable
    data class NodeRegistration(
        val nodeAddress: String,
        val nodeType: String, // "FULL" or "LIGHT"
        val chainSize: Long,
        val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketMessage()
    
    /**
     * Registration acknowledgment from FuelNode
     */
    @Serializable
    data class RegistrationAck(
        val success: Boolean,
        val message: String,
        val registeredNodes: Int
    ) : WebSocketMessage()
    
    /**
     * Chain sync request - asks for blocks from a specific index
     */
    @Serializable
    data class ChainSyncRequest(
        val fromIndex: Long,
        val requestingNode: String
    ) : WebSocketMessage()
    
    /**
     * Chain sync response - provides requested blocks
     */
    @Serializable
    data class ChainSyncResponse(
        val blocks: List<Block>,
        val totalChainSize: Long
    ) : WebSocketMessage()
    
    /**
     * Fuel Node list update
     */
    @Serializable
    data class FuelNodesUpdate(
        val fuelNodes: List<FuelNodeInfo>
    ) : WebSocketMessage()
    
    /**
     * Full Node list update (for peer discovery)
     */
    @Serializable
    data class FullNodesUpdate(
        val fullNodes: List<String> // List of node addresses
    ) : WebSocketMessage()
    
    /**
     * Ping message for keep-alive
     */
    @Serializable
    data class Ping(val timestamp: Long = System.currentTimeMillis()) : WebSocketMessage()
    
    /**
     * Pong response for keep-alive
     */
    @Serializable
    data class Pong(val timestamp: Long = System.currentTimeMillis()) : WebSocketMessage()
    
    /**
     * Error notification
     */
    @Serializable
    data class Error(
        val code: String,
        val message: String
    ) : WebSocketMessage()
}
