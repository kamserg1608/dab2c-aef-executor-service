package ru.sbrf.ufs.dab2c.core.executor.shared.logging.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect.LogParametersPropagatorAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.extractor.api.LogParameterExtractor
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.impl.LogParametersServiceImpl

/**
 * Spring configuration for logging context parameter aspect and extractors for it.
 */
@Configuration
@EnableAspectJAutoProxy
class LoggingConfiguration {

    @Bean
    @Suppress("UNCHECKED_CAST")
    internal fun logParamsPropagator(
        extractors: List<LogParameterExtractor<*>>
    ) = LogParametersServiceImpl(extractors as List<LogParameterExtractor<Any>>)

    @Bean
    internal fun logParamsPropagatorAspect(
        logParamsPropagator: LogParametersServiceImpl,
    ) = LogParametersPropagatorAspect(logParamsPropagator)
}
