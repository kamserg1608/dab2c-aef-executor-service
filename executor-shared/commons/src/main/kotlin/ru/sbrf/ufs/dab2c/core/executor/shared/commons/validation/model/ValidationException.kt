package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model

/**
* Custom exception class to represent validation errors in the application.
 **/
open class ValidationException(
    /** Validation error message. */
    message: String = "Validation failed",
    /** List of violations. */
    val violations: List<Violation> = emptyList()
) : Exception(message) {

    override val message: String
        get() {
            val violationsString =
                violations.joinToString(",\n\t\t") { violation: Violation -> violation.message }

            return "${super.message}:\n\tОтклонения: [$violationsString]"
        }

    override fun fillInStackTrace() = this
}
