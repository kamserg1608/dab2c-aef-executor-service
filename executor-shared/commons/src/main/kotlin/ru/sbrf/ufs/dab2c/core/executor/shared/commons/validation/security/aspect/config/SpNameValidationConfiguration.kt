package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.caller.api.CallerNameExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.CachingSecurityValidatorFactoryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SecurityValidatorFactoryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SpNameValidationAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.mapper.NotAllowedSubsystemExceptionMapper
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * Enables security validation for classes annotated with [SecurityValidated].
 */
@Configuration
class SpNameValidationConfiguration {

    @Bean
    internal fun spNameValidationAspect(
        @Qualifier(SECURITY_VALIDATOR_FACTORY)
        securityValidatorFactory: SecurityValidatorFactory
    ) =
        SpNameValidationAspect(securityValidatorFactory)

    @Bean(SECURITY_VALIDATOR_FACTORY)
    internal fun securityValidatorFactory(
        configService: ExtendedConfigService,
        callerNameExtractor: CallerNameExtractor
    ) =
        CachingSecurityValidatorFactoryImpl(
            SecurityValidatorFactoryImpl(configService, callerNameExtractor)
        )

    @Bean
    internal fun notAllowedSubsystemExceptionMapper() = NotAllowedSubsystemExceptionMapper()

    internal companion object {
        internal const val SECURITY_VALIDATOR_FACTORY = "SECURITY_VALIDATOR_FACTORY"
    }
}
