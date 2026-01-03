package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
        val transaction: String,
        val sender: String,
        val receiver: String,
        val remittance: Double,
        val commission: Double,
        val timestamp: Long = System.currentTimeMillis()
)
