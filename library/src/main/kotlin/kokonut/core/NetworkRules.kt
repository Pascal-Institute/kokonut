package kokonut.core

import kotlinx.serialization.Serializable

/**
 * Network rules defined in Genesis Block These rules are immutable and define the network's
 * fundamental parameters
 */
@Serializable
data class NetworkRules(
        val networkId: String = "kokonut-mainnet",
        val minFuelStake: Double = 1_000_000.0, // Minimum stake to become Fuel Node
        val minFullStake: Double = 1.0, // Minimum stake to become Full Node
        val fuelConsensusThreshold: Double = 0.67, // 2/3 consensus required
        val maxFuelNodes: Int = 100, // Maximum number of Fuel Nodes
        val faucetAmount: Double = 100.0 // Amount given by faucet
)
