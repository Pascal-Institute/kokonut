package kokonut.core

import kotlinx.serialization.Serializable

/** Response object for balance queries */
@Serializable
data class BalanceResponse(val address: String, val balance: Double, val ticker: String = "KNT")
