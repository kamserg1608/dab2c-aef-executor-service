package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params.SpNameValidatorParameters

/**
 * Parameter based security validator that gets a list of allowed subsystems from [parameters]
 * and extracts spName using [callerNameExtractor].
 */
class SpNameSecurityValidator(
    /**
     * Used to provide a list of allowed subsystems.
     */
    private val parameters: SpNameValidatorParameters,
    /**
     * Used to extract spName from instance.
     */
    private val callerNameExtractor: CallerNameExtractor
) : SecurityValidator {

    override fun validate(instance: Any): Boolean {
        val spName = callerNameExtractor.extract(instance)
        val allowedSubsystems = parameters.getAllowedSubsystems()

        return !spName.isNullOrEmpty() && spName in allowedSubsystems
    }
}
