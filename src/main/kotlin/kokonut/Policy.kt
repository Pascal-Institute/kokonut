package kokonut

import kokonut.core.Block
import kotlinx.serialization.Serializable

@Serializable
data class Policy(
    val version : Int,
    val difficulty : Int,
) {
    fun isValid(block: Block): Boolean {
        return (version == block.version) && (difficulty == block.difficulty)
    }
}
