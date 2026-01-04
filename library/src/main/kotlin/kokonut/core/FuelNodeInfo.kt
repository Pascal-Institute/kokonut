package kokonut.core

import kotlinx.serialization.Serializable

/**
 * Fuel Node registration information.
 * 
 * Fuel Nodes are network bootstrap nodes that coordinate consensus
 * and peer discovery. They require a minimum stake to register.
 * 
 * Stored in blockchain blocks (not in Genesis).
 */
@Serializable
data class FuelNodeInfo(
        val address: String,
        val publicKey: String,
        val stake: Double,
        val registeredAt: Long = System.currentTimeMillis(),
        val isBootstrap: Boolean = false // First Fuel Node
)

/**
 * Defines the type of data contained in a block.
 * 
 * Each block type represents a specific operation in the network:
 * - Transactions: Regular value transfers
 * - Staking: Validator registration and stake management  
 * - Node Registry: Fuel/Full node registration and removal
 */
@Serializable
enum class BlockDataType(val description: String) {
    /** Regular transaction - value transfer between accounts */
    TRANSACTION("Regular transaction"),
    
    /** One-time onboarding reward for a new validator */
    VALIDATOR_ONBOARDING("Validator onboarding reward"),
    
    /** Lock stake into stakeVault (validator registration) */
    STAKE_LOCK("Stake lock for validation"),
    
    /** Fuel Node registration in the network */
    FUEL_REGISTRATION("Fuel Node registration"),
    
    /** Fuel Node removal from the network */
    FUEL_REMOVAL("Fuel Node removal"),
    
    /** Full Node registration in the network */
    FULL_REGISTRATION("Full Node registration"),
    
    /** Full Node removal from the network */
    FULL_REMOVAL("Full Node removal"),
    
    /** Retrieve locked stake (stop validating) */
    UNSTAKE("Unstake and stop validating");

    /** Returns true if this is a staking-related operation */
    val isStakingOperation: Boolean
        get() = this == STAKE_LOCK || this == UNSTAKE || this == VALIDATOR_ONBOARDING

    /** Returns true if this is a node registry operation */
    val isNodeRegistryOperation: Boolean
        get() = this in listOf(FUEL_REGISTRATION, FUEL_REMOVAL, FULL_REGISTRATION, FULL_REMOVAL)
}
