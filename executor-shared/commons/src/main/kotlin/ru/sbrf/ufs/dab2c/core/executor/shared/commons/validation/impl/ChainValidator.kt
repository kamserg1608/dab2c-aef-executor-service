package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.api.Validator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.api.ValidatorPart
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.model.ValidationResult

/** A class that implements a chain of validators for a given type, using the chain of [validators]. */
class ChainValidator<T>(
    private val validators: List<ValidatorPart<T>>
) : Validator<T> {

    override fun validate(instance: T): ValidationResult = validators.asSequence()
        .map { it.validate(instance) }
        .reduceOrNull(ValidationResult::plus)
        ?: ValidationResult(valid = true)
}
