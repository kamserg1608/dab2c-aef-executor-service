package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.codec.CodecCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.config.WebFluxConfigurer
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.api.RestObjectMapperProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.customizer.RestObjectMapperCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.impl.RestObjectMapperProviderImpl

/**
 * Configuration class for providing a customized ObjectMapper service bean.
 */
@Configuration
class RestObjectMapperConfiguration {

    @Bean
    fun webFluxCustomizer(objectMapper: ObjectMapper): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                configurer.defaultCodecs().apply {
                    maxInMemorySize(16 * 1024 * 1024)
                    jackson2JsonEncoder(Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON))
                    jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON))
                }
            }
        }
    }

    @Bean
    fun actuatorCodecCustomizer(): CodecCustomizer {
        return CodecCustomizer { configurer ->
            configurer.defaultCodecs().enableLoggingRequestDetails(true)
        }
    }

    @Bean
    fun actuatorWebFluxConfigurer(objectMapper: ObjectMapper): WebFluxConfigurer {
        return object : WebFluxConfigurer {
            override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
                val actuatorV3 = MediaType.parseMediaType("application/vnd.spring-boot.actuator.v3+json")
                val actuatorV2 = MediaType.parseMediaType("application/vnd.spring-boot.actuator.v2+json")

                val encoder = Jackson2JsonEncoder(objectMapper, actuatorV3, actuatorV2, MediaType.APPLICATION_JSON)
                val decoder = Jackson2JsonDecoder(objectMapper, actuatorV3, actuatorV2, MediaType.APPLICATION_JSON)

                configurer.defaultCodecs().jackson2JsonEncoder(encoder)
                configurer.defaultCodecs().jackson2JsonDecoder(decoder)
            }
        }
    }

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
