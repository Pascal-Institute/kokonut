package kokonut.core

import kotlinx.serialization.Serializable

@Serializable
data class BlockData(
    val miner : String  = "",
    val comment: String
)