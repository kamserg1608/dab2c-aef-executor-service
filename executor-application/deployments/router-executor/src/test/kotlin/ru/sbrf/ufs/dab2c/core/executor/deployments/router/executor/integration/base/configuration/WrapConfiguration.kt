package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.cbreaker.api.ServiceStateAgent
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.config.CbulConfiguration.Companion.INITIAL_CBUL_DAO_SERVICE
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.CircuitBreakerParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect.LogParametersPropagatorAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.service.MonitoringServiceAdapter
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.SpykBeanPostProcessor
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

/**
 * Configuration containing names and classes of beans to be spied or mocked.
 */
@Configuration
class WrapConfiguration {

    /**
     * Provides `SpyAndMockBeanPostProcessor` based on configuration.
     */
    @Bean
    internal fun spyAndMockBeanPostProcessor(): SpykBeanPostProcessor {

        val wrapperConfig = SpykBeanConfig(
            spyClasses = listOf(ObjectMapper::class, CircuitBreakerParameters::class,
                InvokeRest::class, ServiceStateAgent::class,
                MonitoringServiceAdapter::class, LogParametersPropagatorAspect::class),
            spyBeanNames = listOf(INITIAL_CBUL_DAO_SERVICE)
        )

        return SpykBeanPostProcessor(wrapperConfig)
    }
}
