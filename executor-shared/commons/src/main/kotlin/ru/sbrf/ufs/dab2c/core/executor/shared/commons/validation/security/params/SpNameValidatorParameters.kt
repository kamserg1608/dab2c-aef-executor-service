package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params

/**
 * [SpNameValidator] parameters.
 */
interface SpNameValidatorParameters {

    /** This method returns allowed subsystems. */
    fun getAllowedSubsystems(): List<String>
}
