package kokonut.util.full

import kotlinx.serialization.Serializable

@Serializable
data class FullNode(
    val ServiceID: String,
    val ServiceName: String,
    val ServiceAddress: String,
    val ServiceWeights: Weights,
)