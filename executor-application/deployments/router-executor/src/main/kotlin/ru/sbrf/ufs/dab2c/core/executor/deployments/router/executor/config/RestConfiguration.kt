package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.api.InvokeProxyService
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.annotation.EnableErrorHandling

/**
 * REST-service configuration.
 */
@Configuration
@EnableErrorHandling
class RestConfiguration {

    @Bean
    internal fun invokeRestService(
        invokeProxyService: InvokeProxyService
    ) = InvokeRest(invokeProxyService)
}
