package kokonut.core

import kotlinx.serialization.Serializable

/**
 * Validator represents a network participant who stakes KNT to validate blocks.
 * 
 * This is an immutable data class representing a validator's state at a point in time.
 * State changes result in new Validator instances rather than mutation.
 */
@Serializable
data class Validator(
        val address: String, // Wallet public key hash
        val stakedAmount: Double, // Amount of KNT staked
        val isActive: Boolean = true, // Can participate in validation
        val blocksValidated: Long = 0, // Total blocks validated
        val rewardsEarned: Double = 0.0 // Total rewards earned
)
