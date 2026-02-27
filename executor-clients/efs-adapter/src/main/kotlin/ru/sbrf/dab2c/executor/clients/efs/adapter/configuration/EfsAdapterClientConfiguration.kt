package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration

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
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties.EfsAdapterClientConfigurationProperties
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * Spring configuration for EFS Adapter HTTP client.
 */
@Configuration
@EnableConfigurationProperties(EfsAdapterClientConfigurationProperties::class)
class EfsAdapterClientConfiguration {

    @Bean(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME)
    internal fun efsAdapterHttpClient(
        properties: EfsAdapterClientConfigurationProperties
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
            maxConnectionsCount = properties.pool.maxConnections
        }
        installPlugins(properties)
    }

    /** Bean name constants. */
    companion object {
        const val EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME = "efsAdapterHttpClient"
    }
}

@Suppress("LongMethod")
private fun io.ktor.client.HttpClientConfig<*>.installPlugins(
    properties: EfsAdapterClientConfigurationProperties
) {
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(ObjectMappers.MAPPER)) }
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
        sanitizeHeader("*****") { it == HttpHeaders.Cookie }
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
