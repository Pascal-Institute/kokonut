package kokonut

import kokonut.core.Block
import kotlinx.serialization.Serializable

@Serializable
data class Policy(
    val version : Int,
    val difficulty : Int,
    val reward: Double,
) {
    fun isValid(block: Block): Boolean {
        return (version == block.version) && (difficulty == block.difficulty) && (reward == block.reward)
    }
}
