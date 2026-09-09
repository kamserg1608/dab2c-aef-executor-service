package ru.sbrf.dab2c.executor.clients.http.factory

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.util.appendIfNameAbsent
import io.opentelemetry.context.Context
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.tracing.propagation.W3CTraceContextInjector
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

/**
 * Builds preconfigured Ktor HTTP clients from named entries of `http.clients`.
 */
class HttpClientFactory(private val properties: HttpClientsProperties) {

    /**
     * Creates an HTTP client configured by the named entry, failing fast if the entry is absent.
     */
    fun create(clientName: String): HttpClient {
        val clientProperties = propertiesFor(clientName)
        return HttpClient(CIO) {
            engine {
                requestTimeout = clientProperties.requestTimeout
                maxConnectionsCount = clientProperties.pool.maxConnections
            }
            installPlugins(clientProperties)
            defaultRequest {
                properties.defaultHeaders.forEach { (name, value) ->
                    if (value.isNotBlank()) {
                        headers.appendIfNameAbsent(name, value)
                    }
                }
            }
        }
    }

    /**
     * Returns the named entry of `http.clients`, failing fast if it is absent.
     */
    fun propertiesFor(clientName: String): HttpClientProperties =
        checkNotNull(properties.clients[clientName]) {
            "No HTTP client configuration for '$clientName' under 'http.clients'"
        }
}

private val otelTraceparentPlugin = createClientPlugin("OtelTraceparent") {
    onRequest { request, _ ->
        W3CTraceContextInjector.inject(Context.current()) { key, value ->
            request.headers.remove(key)
            request.headers.append(key, value)
        }
    }
}

@Suppress("LongMethod")
private fun HttpClientConfig<*>.installPlugins(properties: HttpClientProperties) {
    install(otelTraceparentPlugin)
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(ObjectMappers.MAPPER)) }
    install(Logging) {
        logger = Logger.DEFAULT
        level = properties.logLevel
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
