package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties.EfsAdapterClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.ConfiguratorClientImpl

private val logger = KotlinLogging.logger {}

/**
 * Spring configuration for EFS Adapter API clients.
 */
@Configuration
@EnableConfigurationProperties(EfsAdapterClientConfigurationProperties::class)
class EfsAdapterClientConfiguration {

    @Bean(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME)
    internal fun efsAdapterObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    }

    @Bean(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME)
    internal fun efsAdapterHttpClient(
        properties: EfsAdapterClientConfigurationProperties,
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME)
        objectMapper: ObjectMapper
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
        }

        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(objectMapper))
        }

        install(Logging) {
            level = LogLevel.INFO
            logger = object : Logger {
                override fun log(message: String) {
                    ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.logger.debug { message }
                }
            }
        }

        install(HttpTimeout) {
            connectTimeoutMillis = properties.connectionTimeout
            requestTimeoutMillis = properties.requestTimeout
            socketTimeoutMillis = properties.requestTimeout
        }

        defaultRequest {
            url(properties.baseUrl)
        }
    }

    @Bean
    internal fun configuratorClient(
        @Qualifier(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME)
        httpClient: HttpClient
    ): ConfiguratorClient = ConfiguratorClientImpl(httpClient)

    internal companion object {
        internal const val EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME = "efsAdapterObjectMapper"
        internal const val EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME = "efsAdapterHttpClient"
    }
}
