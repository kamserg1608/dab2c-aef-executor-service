package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config

import org.springframework.context.annotation.Bean
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.SpykBeanPostProcessor
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

class SpyBeanConfiguration {

    @Bean
    internal fun spyConfigurer() = SpykBeanPostProcessor(
        config = SpykBeanConfig(
            spyClasses = listOf(
                InvokeRest::class
            )
        )
    )
}
