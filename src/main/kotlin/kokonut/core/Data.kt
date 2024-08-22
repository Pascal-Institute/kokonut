package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class Data(
    val reward: Double,
    val ticker: String = Identity.ticker,
    val miner: String,
    val transactions : List<Transaction>?,
    val comment: String
)