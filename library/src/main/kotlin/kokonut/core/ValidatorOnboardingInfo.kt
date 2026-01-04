package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class ValidatorOnboardingInfo(
        val validatorAddress: String,
        val fullNodeAddress: String,
        val fuelNodeAddress: String,
        val amountToFullNode: Double = 1.0,
        val amountToValidator: Double = 1.0,
        val totalWithdrawn: Double = 2.0,
        val onboardedAt: Long = System.currentTimeMillis()
)
