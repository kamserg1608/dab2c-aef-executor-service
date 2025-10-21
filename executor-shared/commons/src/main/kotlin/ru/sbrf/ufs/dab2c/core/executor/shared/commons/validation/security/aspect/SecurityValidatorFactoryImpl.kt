package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect

import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.params.SpNameValidatorParametersImpl
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * Default implementation of [SecurityValidatorFactory] caching already retrieved validators.
 */
class SecurityValidatorFactoryImpl(
    /**
     * Configuration service.
     */
    private val configService: ExtendedConfigService,
    /**
     * 'spName'-extractor locator.
     */
    private val callerNameExtractor: CallerNameExtractor
) : SecurityValidatorFactory {

    override fun create(subject: String): SecurityValidator =

        SpNameSecurityValidator(
            parameters = SpNameValidatorParametersImpl(configService, subject),
            callerNameExtractor = callerNameExtractor
        )
}
