package ru.sbrf.dab2c.executor.clients.giga.agent.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties.GigaVoiceAgentClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.giga.agent.impl.GigaVoiceAgentClientImpl
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * Spring configuration for GigaVoice Agent API client.
 */
@Configuration
@EnableConfigurationProperties(GigaVoiceAgentClientConfigurationProperties::class)
class GigaVoiceAgentClientConfiguration {

    @Bean(GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME)
    internal fun gigaVoiceAgentObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    }

    @Bean(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME)
    internal fun gigaVoiceAgentHttpClient(
        properties: GigaVoiceAgentClientConfigurationProperties,
        @Qualifier(GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
            maxConnectionsCount = properties.pool.maxConnections
        }
        installPlugins(properties, objectMapper)
    }

    @Bean
    internal fun gigaVoiceAgentClient(
        @Qualifier(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        @Qualifier(GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper,
        properties: GigaVoiceAgentClientConfigurationProperties,
        settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
        functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
        monitoringServiceFactory: MonitoringServiceFactory
    ): GigaVoiceAgentClient = GigaVoiceAgentClientImpl(
        httpClient,
        objectMapper,
        properties.baseUrl,
        settingsRequestBuilder,
        functionCallRequestBuilder,
        monitoringServiceFactory
    )

    internal companion object {
        internal const val GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME = "gigaVoiceAgentClientObjectMapper"
        internal const val GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME = "gigaVoiceAgentClientHttpClient"
    }
}

@Suppress("LongMethod")
private fun io.ktor.client.HttpClientConfig<*>.installPlugins(
    properties: GigaVoiceAgentClientConfigurationProperties,
    objectMapper: ObjectMapper
) {
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
    install(HttpTimeout) {
        connectTimeoutMillis = properties.connectionTimeout
        requestTimeoutMillis = properties.requestTimeout
        socketTimeoutMillis = properties.socketTimeout
    }
    install(HttpRequestRetry) {
        maxRetries = properties.retry.maxRetries
        retryIf { _, response -> response.status.value in properties.retry.statusCodes }
        retryOnExceptionIf { _, cause -> cause is ConnectException || cause is SocketTimeoutException }
        delayMillis { retry ->
            val delay = properties.retry.delay * properties.retry.multiplier.pow((retry - 1).toDouble())
            delay.toLong().coerceAtMost(properties.retry.maxDelay)
        }
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                val bodyText = response.bodyAsText()
                throw ResponseException(response, bodyText)
            }
        }
    }
}
