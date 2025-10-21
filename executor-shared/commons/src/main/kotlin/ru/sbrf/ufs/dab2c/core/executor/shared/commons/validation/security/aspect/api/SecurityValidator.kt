package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api

/**
 * This interface checks whether caller system has access to perform action.
 */
interface SecurityValidator {

    /**
     * Validates a given instance.
     *
     * @return true, if caller system has access to perform action, false - otherwise
     */
    fun validate(instance: Any): Boolean
}
