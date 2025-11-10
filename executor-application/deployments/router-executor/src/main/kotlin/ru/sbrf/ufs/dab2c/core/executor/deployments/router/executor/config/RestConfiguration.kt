package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.annotation.EnableErrorHandling

/**
 * REST-service configuration.
 */
@Configuration
@EnableErrorHandling
class RestConfiguration {

    @Bean
    internal fun invokeRestService() = InvokeRest()
}
