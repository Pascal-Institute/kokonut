package kokonut.core

import kotlinx.serialization.Serializable

/** Network policy for PoS blockchain Used for API responses containing policy information */
@Serializable data class Policy(val minimumStake: Double = 1.0)
