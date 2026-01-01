package kokonut.core

import kotlinx.serialization.Serializable

/** Fuel Node registration information Stored in blockchain blocks (not in Genesis) */
@Serializable
data class FuelNodeInfo(
        val address: String,
        val publicKey: String,
        val stake: Double,
        val registeredAt: Long = System.currentTimeMillis(),
        val isBootstrap: Boolean = false // First Fuel Node
)

/** Block data types for different operations */
@Serializable
enum class BlockDataType {
    TRANSACTION, // Regular transaction
    VALIDATOR_ONBOARDING, // 1-time onboarding reward for a validator
    FUEL_REGISTRATION, // Fuel Node registration
    FUEL_REMOVAL, // Fuel Node removal
    FULL_REGISTRATION, // Full Node registration
    FULL_REMOVAL // Full Node removal
}
