package kokonut.core

import kotlinx.serialization.Serializable

/**
 * Handshake request from Light Node to Full Node
 * This initiates the connection ritual between nodes
 */
@Serializable
data class HandshakeRequest(
        val nodeType: String, // "LIGHT", "FULL", etc.
        val publicKey: String, // REQUIRED: Light Node's public key for authentication
        val clientVersion: Int, // Protocol version of the client
        val timestamp: Long = System.currentTimeMillis()
)

/**
 * Handshake response from Full Node to Light Node
 * Contains essential network information for the Light Node to verify
 */
@Serializable
data class HandshakeResponse(
        val success: Boolean,
        val message: String,
        val networkInfo: NetworkInfo? = null
)

/**
 * Network information provided during handshake
 */
@Serializable
data class NetworkInfo(
        val nodeType: String, // "FUEL", "FULL"
        val networkId: String, // e.g., "kokonut-mainnet"
        val genesisHash: String, // Genesis block hash for verification
        val chainSize: Long, // Current blockchain height
        val protocolVersion: Int, // Server's protocol version
        val totalValidators: Int, // Number of active validators
        val totalCurrencyVolume: Double, // Total KNT in circulation
        val connectedFuelNodes: Int, // Number of known Fuel Nodes
        val serverTimestamp: Long = System.currentTimeMillis()
)
