package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api

/**
 * Represents a factory for [SecurityValidator].
 */
interface SecurityValidatorFactory {

    /**
     * Creates a [SecurityValidator] for the given service name.
     *
     * @param subject service name
     * @return [SecurityValidator]
     */
    fun create(subject: String): SecurityValidator
}
