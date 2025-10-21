package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.json

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource

object JsonUtil {

    private val objectMapper = ObjectMapper()
        .registerModule(JavaTimeModule())
        .registerModule(KotlinModule.Builder().build())

    private val typeFactory = objectMapper.typeFactory

    fun readJsonFromFile(pathToFile: String): String {
        val inputStream = this::class.java.classLoader.getResourceAsStream(pathToFile)
        val jsonBytes = inputStream.use { it!!.readBytes() }
        return String(jsonBytes, Charsets.UTF_8)
    }

    fun readTree(string: String): JsonNode = objectMapper.readTree(string)

    fun valueToTree(value: Any): JsonNode = objectMapper.valueToTree(value)

    fun load(pathToFile: String): JsonNode = objectMapper.readTree(readJsonFromFile(pathToFile))

    fun serialize(instance: Any?): String = objectMapper.writeValueAsString(instance)

    fun <T> deserializePath(path: String, make: TypeFactory.() -> JavaType): T =
        ClassPathResource(path).deserialize(typeFactory.make())

    fun javaType(make: TypeFactory.() -> JavaType): JavaType = typeFactory.make()

    fun <T> Resource.deserialize(javaType: JavaType): T = inputStream.use { inputStreamInstance ->
        objectMapper.readValue(inputStreamInstance, javaType)
    }

    fun <T> String.deserialize(javaType: JavaType): T = objectMapper.readValue(this, javaType)
}
