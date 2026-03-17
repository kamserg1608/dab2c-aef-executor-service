package ru.sbrf.dab2c.executor.library.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule

/**
 * Shared pre-configured ObjectMapper instances for the application.
 */
object ObjectMappers {

    val MAPPER: ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    }
}
