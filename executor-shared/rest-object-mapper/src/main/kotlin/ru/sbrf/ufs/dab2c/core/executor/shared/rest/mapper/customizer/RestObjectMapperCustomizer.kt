package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.customizer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.reactive.config.WebFluxConfigurer

/**
 * Configuration class for replacing default converter created by Spring MVC.
 */
class RestObjectMapperCustomizer(private val objectMapper: ObjectMapper) : WebFluxConfigurer {

    /**
     * Configures `objectMapper` converter to default one.
     */
    override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
        val jacksonConverter = MappingJackson2HttpMessageConverter()
        jacksonConverter.objectMapper = objectMapper
        configurer.defaultCodecs().jackson2JsonEncoder(
            Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON)
        )
        configurer.defaultCodecs().jackson2JsonDecoder(
            Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON)
        )
    }
}
