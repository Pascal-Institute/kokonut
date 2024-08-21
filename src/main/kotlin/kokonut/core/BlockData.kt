package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class BlockData(
    val reward : Double,
    val miner : String,
    val comment: String
)
