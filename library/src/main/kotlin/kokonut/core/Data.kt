package kokonut.core

import kokonut.core.BlockChain.Companion.TICKER
import kotlinx.serialization.Serializable

@Serializable
data class Data(
        var reward: Double = 0.0,
        val ticker: String = TICKER,
        val validator: String = "", // Changed from 'miner' to 'validator' for PoS
        var transactions: List<Transaction> = emptyList(),
        val comment: String = "",

        // Block type and metadata
        val type: BlockDataType = BlockDataType.TRANSACTION,
        val validatorOnboardingInfo: ValidatorOnboardingInfo? = null,
        val fuelNodeInfo: FuelNodeInfo? = null, // For FUEL_REGISTRATION/REMOVAL
        val networkRules: NetworkRules? = null // Only for Genesis Block
)
