package kokonut

import kotlinx.serialization.Serializable

@Serializable
data class Fullnode(
    val ServiceID: String,
    val ServiceName: String,
    val ServiceAddress: String,
    val ServiceWeights: Weights
)