package ru.sbrf.dab2c.executor.clients.iag.iag

import io.ktor.client.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.http.factory.HttpClientFactory
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.iag.impl.IagFunctionClientImpl
import ru.sbrf.dab2c.executor.clients.iag.monitoring.MonitoringIagFunctionClientDecorator
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory

/**
 * Spring configuration for IAG API client.
 */
@Configuration
class IagClientConfiguration {

    @Bean(IAG_HTTP_CLIENT_BEAN_NAME)
    internal fun iagHttpClient(httpClientFactory: HttpClientFactory): HttpClient =
        httpClientFactory.create(IAG_CLIENT_NAME)

    @Bean
    internal fun iagFunctionCallClient(
        @Qualifier(IAG_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        httpClientFactory: HttpClientFactory,
        functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
        metricFactory: MetricFactory
    ): IagFunctionClient {
        val baseUrl = httpClientFactory.propertiesFor(IAG_CLIENT_NAME).baseUrl
        val impl = IagFunctionClientImpl(
            httpClient, ObjectMappers.MAPPER, baseUrl, functionCallRequestBuilder
        )

        return MonitoringIagFunctionClientDecorator(impl, metricFactory)
    }

    /** Bean and client name constants. */
    companion object {
        const val IAG_HTTP_CLIENT_BEAN_NAME = "iagHttpClient"
        const val IAG_CLIENT_NAME = "iag"
    }
}
