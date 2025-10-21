package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.config

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SpNameValidationAspect
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * Options for [SpNameValidationAspect].
 */
data class SpNameValidationOptions(
    /**
     * 'spName' locator.
     */
    val callerNameExtractor: CallerNameExtractor,
    /**
     * Config service.
     */
    val configService: ExtendedConfigService
)
