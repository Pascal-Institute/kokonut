package kokonut

import kotlinx.serialization.Serializable

@Serializable
data class Fullnode(
    val ID: String,
    val Service: String,
    val Tags: List<String>,
    val Address: String,
    val Meta: Map<String, String>,
    val Port: Int,
    val Weights: Weights
)