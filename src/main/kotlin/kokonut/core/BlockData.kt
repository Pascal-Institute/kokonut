package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class BlockData(
    val reward : Double,
    val ticker : String = Identity.ticker,
    val miner : String,
    val comment: String
)
