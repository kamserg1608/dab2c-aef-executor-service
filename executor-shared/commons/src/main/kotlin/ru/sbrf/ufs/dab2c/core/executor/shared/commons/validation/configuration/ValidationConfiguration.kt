package ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.validation.impl.ValidationExceptionMapper

/**
 * Validation configuration.
 */
@Configuration
class ValidationConfiguration {

    @Bean
    internal fun validationExceptionMapper() = ValidationExceptionMapper()
}
