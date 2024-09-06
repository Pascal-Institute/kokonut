package kokonut

import kotlinx.serialization.Serializable

@Serializable
data class Weights(
    val Passing: Int,
    val Warning: Int
)
