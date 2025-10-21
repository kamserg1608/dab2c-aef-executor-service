package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.api.Validator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model.ValidationResult
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model.Violation

/** Default implementation of [Validator] for any class.  */
abstract class AbstractNumericIdValidator<T> : Validator<T> {

    /** [T] extension, that extracts numeric id. */
    abstract fun T.extractNumericId(): String?

    /** Defines violation message. */
    abstract fun violationMessage(numericId: String?): String

    override fun validate(instance: T): ValidationResult {
        val violations = mutableListOf<Violation>()
        val numericId = instance.extractNumericId()

        if (numericId == null || !numericId.isDigit()) {
            violations.add(
                Violation(violationMessage(numericId))
            )
        }
        return ValidationResult(valid = violations.isEmpty(), violations = violations)
    }

    private fun String.isDigit() = isNotEmpty() && all { it.isDigit() }
}
