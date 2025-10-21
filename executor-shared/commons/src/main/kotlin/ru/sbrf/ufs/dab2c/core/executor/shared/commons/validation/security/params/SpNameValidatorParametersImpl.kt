package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.util.extension.getStringList
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * A class that implements the logic of getting a list of subsystems for different operation types.
 */
class SpNameValidatorParametersImpl(
    private val configService: ExtendedConfigService,
    private val allowedSubsystemsParameter: String
) : SpNameValidatorParameters {

    override fun getAllowedSubsystems(): List<String> = configService.getStringList(allowedSubsystemsParameter)
}
