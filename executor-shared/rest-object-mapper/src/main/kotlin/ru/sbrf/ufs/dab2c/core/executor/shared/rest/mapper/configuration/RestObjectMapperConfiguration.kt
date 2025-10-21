package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.api.RestObjectMapperProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.customizer.RestObjectMapperCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.impl.RestObjectMapperProviderImpl

/**
 * Configuration class for providing a customized ObjectMapper service bean.
 */
@Configuration
class RestObjectMapperConfiguration {

    /**
     * Defines bean that customizes provided `ObjectMapper`.
     */
    @Bean
    internal fun objectMapperCustomizer(objectMapper: ObjectMapper): RestObjectMapperCustomizer =
        RestObjectMapperCustomizer(objectMapper)

    /**
     * Provides a customized `ObjectMapper` bean for the application context.
     */
    @Bean
    internal fun objectMapper(restObjectMapperProvider: RestObjectMapperProvider): ObjectMapper =
        restObjectMapperProvider.getMapper()

    /**
     * Provides an ObjectMapper service for the application context.
     */
    @Bean
    internal fun objectMapperService(): RestObjectMapperProvider = RestObjectMapperProviderImpl()
}
