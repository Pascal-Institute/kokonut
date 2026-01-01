package kokonut

import kokonut.core.Block
import kotlinx.serialization.Serializable

/** Network policy for PoS blockchain No difficulty needed in PoS */
@Serializable
data class Policy(
        val minimumStake: Double = 1.0, // Minimum KNT to become validator
) {
    fun isValid(block: Block): Boolean {
        // Version check removed
        return true
    }
}
