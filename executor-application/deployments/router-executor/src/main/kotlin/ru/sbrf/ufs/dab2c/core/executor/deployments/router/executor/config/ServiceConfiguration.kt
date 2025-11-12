package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.api.InvokeProxyService
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.impl.InvokeProxyServiceImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.DefaultLocalTimeProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.time.LocalTimeProvider

/**
 * Service configuration.
 */
@Configuration
class ServiceConfiguration {

    @Bean
    internal fun localTimeProvider(): LocalTimeProvider = DefaultLocalTimeProvider()

    @Bean
    internal fun invokeProxyService(): InvokeProxyService = InvokeProxyServiceImpl()

}
