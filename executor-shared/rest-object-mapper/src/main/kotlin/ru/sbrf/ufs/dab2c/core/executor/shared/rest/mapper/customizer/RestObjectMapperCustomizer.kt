package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.customizer

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Configuration class for replacing default converter created by Spring MVC.
 */
class RestObjectMapperCustomizer(private val objectMapper: ObjectMapper) : WebMvcConfigurer {

    /**
     * Configures `objectMapper` converter to default one.
     */
    override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val jacksonConverter = MappingJackson2HttpMessageConverter()
        jacksonConverter.objectMapper = objectMapper
        converters.add(jacksonConverter)
    }
}
