package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.api

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model.ValidationResult

/** Validator part interface. */
interface ValidatorPart<T> {

    /**
     * Validates the given instance.
     *
     * @param instance the instance to validate
     * @return the validation result
     */
    fun validate(instance: T): ValidationResult
}
