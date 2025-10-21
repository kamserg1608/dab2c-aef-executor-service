package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.api

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl.ChainValidator

/**
 * Represents a validator.
 */
fun interface Validator<T> : ValidatorPart<T> {

    /**
     * Companion object.
     */
    companion object {
        /** Method, for chaining multiple [Validator] into combines one. */
        fun <T> chain(validators: List<ValidatorPart<T>>) = ChainValidator(validators)
    }
}
