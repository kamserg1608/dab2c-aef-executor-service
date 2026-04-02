package ru.sbrf.dab2c.executor.clients.giga.agent.configuration

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
import ru.sbrf.dab2c.executor.clients.giga.agent.audit.AgentInteractionAuditor
import ru.sbrf.dab2c.executor.clients.giga.agent.audit.AuditedGigaVoiceAgentClientDecorator
import ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties.GigaVoiceAgentClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.giga.agent.impl.GigaVoiceAgentClientImpl
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.monitoring.MonitoringGigaVoiceAgentClientDecorator
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * Spring configuration for GigaVoice Agent API client.
 */
@Configuration
@EnableConfigurationProperties(GigaVoiceAgentClientConfigurationProperties::class)
class GigaVoiceAgentClientConfiguration {

    @Bean(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME)
    internal fun gigaVoiceAgentHttpClient(
        properties: GigaVoiceAgentClientConfigurationProperties
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
            maxConnectionsCount = properties.pool.maxConnections
        }
        installPlugins(properties)
    }

    @Suppress("LongParameterList")
    @Bean
    internal fun gigaVoiceAgentClient(
        @Qualifier(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        properties: GigaVoiceAgentClientConfigurationProperties,
        settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
        functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
        metricFactory: MetricFactory,
        agentInteractionAuditor: AgentInteractionAuditor
    ): GigaVoiceAgentClient {

        val impl = GigaVoiceAgentClientImpl(
            httpClient, ObjectMappers.MAPPER, properties.baseUrl, settingsRequestBuilder, functionCallRequestBuilder
        )

        val monitored = MonitoringGigaVoiceAgentClientDecorator(
            impl, metricFactory
        )

        return AuditedGigaVoiceAgentClientDecorator(
            monitored, agentInteractionAuditor, ObjectMappers.MAPPER, properties.baseUrl
        )
    }

    internal companion object {
        internal const val GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME = "gigaVoiceAgentClientHttpClient"
    }
}

@Suppress("LongMethod")
private fun io.ktor.client.HttpClientConfig<*>.installPlugins(
    properties: GigaVoiceAgentClientConfigurationProperties
) {
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(ObjectMappers.MAPPER)) }
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
