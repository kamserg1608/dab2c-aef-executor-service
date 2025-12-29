package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.configuration

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
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.configuration.properties.GigaVoiceAgentClientConfigurationProperties
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.impl.GigaVoiceAgentClientImpl

private val logger = KotlinLogging.logger {}

/**
 * Spring configuration for GigaVoice Agent API client.
 */
@Configuration
@EnableConfigurationProperties(GigaVoiceAgentClientConfigurationProperties::class)
class GigaVoiceAgentClientConfiguration {

    @Bean(GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME)
    internal fun gigaVoiceAgentObjectMapper(): ObjectMapper {
        return ObjectMapper().apply {
            registerModule(kotlinModule())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        }
    }

    @Bean(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME)
    internal fun gigaVoiceAgentHttpClient(
        properties: GigaVoiceAgentClientConfigurationProperties,
        @Qualifier(GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME)
        objectMapper: ObjectMapper
    ): HttpClient {
        return HttpClient(CIO) {
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
                        ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.configuration.logger.debug { message }
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
    }

    @Bean
    internal fun gigaVoiceAgentClient(
        @Qualifier(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME)
        httpClient: HttpClient
    ): GigaVoiceAgentClient = GigaVoiceAgentClientImpl(httpClient)

    internal companion object {
        internal const val GIGA_VOICE_AGENT_OBJECT_MAPPER_BEAN_NAME = "gigaVoiceAgentClientObjectMapper"
        internal const val GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME = "gigaVoiceAgentClientObjectMapper"
    }

}
