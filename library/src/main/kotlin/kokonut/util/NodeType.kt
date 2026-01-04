package kokonut.util

/**
 * Defines the type of node in the Kokonut network.
 * 
 * Each node type has distinct responsibilities:
 * - FUEL: Bootstrap node, maintains network consensus and peer discovery
 * - FULL: Validates transactions and stores complete blockchain
 * - LIGHT: Lightweight client for wallet operations
 */
enum class NodeType(val description: String) {
    FUEL("Fuel Node - Network bootstrap and consensus coordinator"),
    FULL("Full Node - Transaction validation and blockchain storage"),
    LIGHT("Light Node - Lightweight wallet client");

    /** Returns true if this node type can create genesis blocks */
    val canCreateGenesis: Boolean
        get() = this == FUEL

    /** Returns true if this node type participates in validation */
    val participatesInValidation: Boolean
        get() = this == FULL || this == FUEL
}