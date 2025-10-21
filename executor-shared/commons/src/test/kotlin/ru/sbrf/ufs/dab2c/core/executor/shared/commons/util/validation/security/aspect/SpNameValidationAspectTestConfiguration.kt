package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.validation.security.aspect

import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.SpNameValidationAspect
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidator
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.security.aspect.api.SecurityValidatorFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.SpykBeanPostProcessor
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.postprocessor.api.SpykBeanConfig

@Configuration
@EnableAspectJAutoProxy
internal class SpNameValidationAspectTestConfiguration {

    @Bean
    internal fun securityValidator() = mockk<SecurityValidator>()

    @Bean
    internal fun securityValidatorFactory() = mockk<SecurityValidatorFactory>()

    @Bean
    internal fun aspect(validatorFactory: SecurityValidatorFactory) = SpNameValidationAspect(validatorFactory)

    @Bean
    internal fun validatedService() = SpNameValidatedService()

    @Bean
    internal fun spyBeanPostProcessor() = SpykBeanPostProcessor(
        SpykBeanConfig(
            spyClasses = listOf(SecurityValidatorFactory::class)
        )
    )
}
