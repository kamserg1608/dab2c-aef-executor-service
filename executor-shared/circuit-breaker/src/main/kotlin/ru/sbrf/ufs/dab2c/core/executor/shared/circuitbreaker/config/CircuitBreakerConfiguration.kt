package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.aspect.CircuitBreakerAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.CircuitBreakerParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.CircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.GlobalRecordedExceptionsCircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerRegistry
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.CircuitBreakerService
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.CallNotPermittedExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.CircuitBreakerRegistryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl.CircuitBreakerServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService

/**
 * Circuit breaker configuration.
 */
@Configuration
class CircuitBreakerConfiguration {

    @Bean
    internal fun circuitBreakerAspect(
        circuitbreakerService: CircuitBreakerService
    ) = CircuitBreakerAspect(circuitbreakerService)

    @Bean
    internal fun circuitBreakerService(
        circuitbreakerRegistry: CircuitBreakerRegistry,
        localTimeProvider: LocalTimeProvider
    ) = CircuitBreakerServiceImpl(circuitbreakerRegistry, localTimeProvider)

    @Bean
    internal fun circuitbreakerRegistry(
        circuitBreakerParameters: CircuitBreakerParameters
    ) =
        CircuitBreakerRegistryImpl(circuitBreakerParameters)

    @Bean
    internal fun circuitBreakerParameters(
        configService: ExtendedConfigService,
        customizers: List<CircuitBreakerConfigCustomizer>
    ) = CircuitBreakerParameters(configService, customizers)

    @Bean
    internal fun defaultIgnoredExceptionsCircuitBreakerConfigCustomizer(): CircuitBreakerConfigCustomizer =
        GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer(emptySet())

    @Bean
    internal fun defaultRecordedExceptionsCircuitBreakerConfigCustomizer(): CircuitBreakerConfigCustomizer =
        GlobalRecordedExceptionsCircuitBreakerConfigCustomizer()

    @Bean
    internal fun callNotPermittedExceptionMapper() = CallNotPermittedExceptionMapper()
}
