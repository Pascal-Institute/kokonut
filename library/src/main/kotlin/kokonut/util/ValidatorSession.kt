package kokonut.util

import kokonut.state.ValidatorState
import kotlinx.serialization.Serializable

@Serializable
data class ValidatorSession(
        val validatorAddress: String,
        val ip: String,
        var validationState: ValidatorState
)
