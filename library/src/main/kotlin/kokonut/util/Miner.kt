package kokonut.util

import kokonut.state.MiningState
import kotlinx.serialization.Serializable

@Serializable
data class Miner(
    val miner : String,
    val ip : String,
    var miningState: MiningState
)
