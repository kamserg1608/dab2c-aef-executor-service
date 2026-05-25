package ru.sbrf.dab2c.executor.clients.iag.iag

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
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
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.iag.iag.properties.IagClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.iag.impl.IagFunctionClientImpl
import ru.sbrf.dab2c.executor.clients.iag.monitoring.MonitoringIagFunctionClientDecorator
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * Spring configuration for IAG API client.
 */
@Configuration
@EnableConfigurationProperties(IagClientConfigurationProperties::class)
class IagClientConfiguration {

    @Bean(IAG_HTTP_CLIENT_BEAN_NAME)
    internal fun iagHttpClient(
        properties: IagClientConfigurationProperties
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
            maxConnectionsCount = properties.pool.maxConnections
        }
        installPlugins(properties)
    }

    @Bean
    internal fun iagFunctionCallClient(
        @Qualifier(IAG_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        properties: IagClientConfigurationProperties,
        functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
        metricFactory: MetricFactory
    ): IagFunctionClient {
        val impl = IagFunctionClientImpl(
            httpClient, ObjectMappers.MAPPER, properties.baseUrl, functionCallRequestBuilder
        )

        return MonitoringIagFunctionClientDecorator(impl, metricFactory)
    }

    /** Bean name constants. */
    companion object {
        const val IAG_HTTP_CLIENT_BEAN_NAME = "iagHttpClient"
    }
}

@Suppress("LongMethod")
private fun HttpClientConfig<*>.installPlugins(
    properties: IagClientConfigurationProperties
) {
    install(ContentNegotiation) {
        register(
            ContentType.Application.Json, JacksonConverter(ObjectMappers.MAPPER)
        )
    }

    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.HEADERS
    }

    install(HttpTimeout) {
        connectTimeoutMillis = properties.connectionTimeout
        requestTimeoutMillis = properties.requestTimeout
        socketTimeoutMillis = properties.socketTimeout
    }

    install(HttpRequestRetry) {
        maxRetries = properties.retry.maxRetries

        retryIf { _, response ->
            response.status.value in properties.retry.statusCodes
        }

        retryOnExceptionIf { _, cause ->
            cause is ConnectException || cause is SocketTimeoutException
        }

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
