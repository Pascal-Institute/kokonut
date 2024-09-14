package kokonut.util

import kokonut.state.MiningState
import kotlinx.serialization.Serializable

@Serializable
data class Miner(
    val id : String,
    var miningState: MiningState
)
