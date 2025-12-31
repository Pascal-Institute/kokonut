package kokonut.core

import kotlinx.serialization.Serializable

/** Validator represents a network participant who stakes KNT to validate blocks */
@Serializable
data class Validator(
        val address: String, // Wallet public key hash
        var stakedAmount: Double, // Amount of KNT staked
        var isActive: Boolean = true, // Can participate in validation
        var blocksValidated: Long = 0, // Total blocks validated
        var rewardsEarned: Double = 0.0 // Total rewards earned
)
