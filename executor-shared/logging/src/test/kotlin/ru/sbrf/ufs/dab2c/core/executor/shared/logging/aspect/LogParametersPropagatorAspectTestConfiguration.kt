package ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.api.LogParametersService
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.SpykBeanPostProcessor
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

@Configuration
@EnableAspectJAutoProxy
class LogParametersPropagatorAspectTestConfiguration {

    @Bean
    internal fun logController() = LogParametersPropagatorController()

    @Bean
    internal fun logParametersService() = mockk<LogParametersService>()

    @Bean
    internal fun logParametersPropagatorAspect(
        logParametersService: LogParametersService
    ) = LogParametersPropagatorAspect(logParametersService)

    @Bean
    internal fun spyBeanPostProcessor() = SpykBeanPostProcessor(
        SpykBeanConfig(
            spyClasses = listOf(
                LogParametersService::class)
        )
    )
}
