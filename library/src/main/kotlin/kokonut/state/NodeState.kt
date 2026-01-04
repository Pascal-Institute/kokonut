package kokonut.state

/**
 * Represents the connection and validation state of a blockchain node.
 * 
 * States:
 * - VALID: Connected and blockchain is valid
 * - INVALID: Connected but blockchain validation failed
 * - DISCONNECTED: Not connected to the network
 */
enum class NodeState(val displayName: String) {
    VALID("Connected & Valid"),
    INVALID("Connected but Invalid"),
    DISCONNECTED("Disconnected");

    /** Returns true if node is connected to the network */
    val isConnected: Boolean
        get() = this != DISCONNECTED

    /** Returns true if node is healthy (connected and valid) */
    val isHealthy: Boolean
        get() = this == VALID
}