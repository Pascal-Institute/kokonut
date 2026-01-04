package kokonut.state

/**
 * Represents the current state of a validator during the validation lifecycle.
 * 
 * State transitions:
 * - READY -> VALIDATING (when validation starts)
 * - VALIDATING -> VALIDATED (on success) or FAILED (on error)
 * - VALIDATED/FAILED -> READY (reset for next validation)
 */
enum class ValidatorState(val displayName: String) {
    READY("Ready to validate"),
    VALIDATING("Validation in progress"),
    VALIDATED("Validation successful"),
    FAILED("Validation failed");

    /** Returns true if validator is currently processing */
    val isProcessing: Boolean
        get() = this == VALIDATING

    /** Returns true if validation has completed (either success or failure) */
    val isComplete: Boolean
        get() = this == VALIDATED || this == FAILED

    /** Returns true if validation was successful */
    val isSuccess: Boolean
        get() = this == VALIDATED
}
