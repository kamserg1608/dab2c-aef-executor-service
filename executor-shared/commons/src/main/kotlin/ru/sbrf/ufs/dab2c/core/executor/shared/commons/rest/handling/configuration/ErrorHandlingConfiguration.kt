package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.api.ExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.NotAcceptableExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.RestExceptionHandlingAdvice
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.UfsIdNotFoundExceptionMapper

/** Configuration for [RestExceptionHandlingAdvice] and default [ExceptionMapper]s. */
@Configuration
class ErrorHandlingConfiguration {

    @Bean
    internal fun restExceptionHandlingAdvice(
        exceptionMappers: List<ExceptionMapper<out Throwable>>
    ) = RestExceptionHandlingAdvice(exceptionMappers)

    @Bean
    internal fun notAcceptableExceptionMapper() = NotAcceptableExceptionMapper()

    @Bean
    internal fun ufsIdNotFoundExceptionMapper() = UfsIdNotFoundExceptionMapper()
}
