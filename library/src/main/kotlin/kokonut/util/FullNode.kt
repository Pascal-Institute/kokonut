package kokonut.util

import kotlinx.serialization.Serializable

@Serializable
data class FullNode(
    val id: String,
    val address: String,
)