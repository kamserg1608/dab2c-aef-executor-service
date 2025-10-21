package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model

/**
 * Represents a validation result.
 */
@Suppress("DataClassContainsFunctions")
data class ValidationResult(
    /**
     * Indicates whether the result is valid.
     */
    val valid: Boolean,
    /**
     * Contains a list of violations.
     */
    val violations: List<Violation> = emptyList()
) {

    /**
     * Merges a given [validationResult] with this one.
     */
    operator fun plus(validationResult: ValidationResult) =
        ValidationResult(
            valid = valid && validationResult.valid,
            violations = violations + validationResult.violations
        )
}
