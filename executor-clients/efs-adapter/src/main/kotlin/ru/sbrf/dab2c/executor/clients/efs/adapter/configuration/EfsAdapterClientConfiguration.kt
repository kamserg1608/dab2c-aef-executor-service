package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
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
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.AuditClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.configuration.properties.EfsAdapterClientConfigurationProperties
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.AuditClientImpl
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.ConfiguratorClientImpl
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.SdsClientImpl
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.TypedSdsClientImpl
import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.math.pow

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
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper
    ): HttpClient = HttpClient(CIO) {
        engine {
            requestTimeout = properties.requestTimeout
            maxConnectionsCount = properties.pool.maxConnections
        }
        installPlugins(properties, objectMapper)
        defaultRequest { url(properties.baseUrl) }
    }

    @Bean
    internal fun configuratorClient(
        @Qualifier(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper,
        properties: EfsAdapterClientConfigurationProperties
    ): ConfiguratorClient = ConfiguratorClientImpl(httpClient, objectMapper, properties.baseUrl)

    @Bean
    internal fun auditClient(
        @Qualifier(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper,
        properties: EfsAdapterClientConfigurationProperties
    ): AuditClient = AuditClientImpl(httpClient, objectMapper, properties.baseUrl)

    @Bean
    internal fun sdsClient(
        @Qualifier(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper,
        properties: EfsAdapterClientConfigurationProperties
    ): SdsClient = SdsClientImpl(httpClient, objectMapper, properties.baseUrl)

    @Bean
    internal fun typedSdsClient(
        sdsClient: SdsClient,
        @Qualifier(EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper
    ): TypedSdsClient = TypedSdsClientImpl(sdsClient, objectMapper)

    internal companion object {
        internal const val EFS_ADAPTER_OBJECT_MAPPER_BEAN_NAME = "efsAdapterObjectMapper"
        internal const val EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME = "efsAdapterHttpClient"
    }
}

@Suppress("LongMethod")
private fun io.ktor.client.HttpClientConfig<*>.installPlugins(
    properties: EfsAdapterClientConfigurationProperties,
    objectMapper: ObjectMapper
) {
    install(ContentNegotiation) { register(ContentType.Application.Json, JacksonConverter(objectMapper)) }
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
