package ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.api.RestObjectMapperProvider
import ru.sbrf.ufs.platform.core.json.DefaultJsonMapperLocator
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

/**
 * Implementation of [RestObjectMapperProvider].
 */
class RestObjectMapperProviderImpl : RestObjectMapperProvider {
    override fun getMapper(): ObjectMapper =
        DefaultJsonMapperLocator().mapper
            .registerModules(KotlinModule.Builder().build())
            .registerModules(createCustomJavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    private fun createCustomJavaTimeModule(): JavaTimeModule {
        val formatter = DateTimeFormatterBuilder()
            .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            .optionalStart()
            .appendLiteral('Z')
            .optionalEnd()
            .toFormatter()

        return JavaTimeModule().apply {
            addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer(formatter))
            addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer(formatter))
        }
    }
}
