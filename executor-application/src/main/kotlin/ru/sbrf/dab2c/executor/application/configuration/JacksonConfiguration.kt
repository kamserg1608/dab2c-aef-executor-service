package ru.sbrf.dab2c.executor.application.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Jackson configuration providing primary ObjectMapper for the application.
 */
@Configuration
class JacksonConfiguration {

    /**
     * Primary ObjectMapper for the application.
     */
    @Bean
    @Primary
    fun objectMapper(): ObjectMapper = ObjectMappers.MAPPER
}
