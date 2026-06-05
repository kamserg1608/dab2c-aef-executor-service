package ru.sbrf.dab2c.executor.clients.configurator.configuration

import io.ktor.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.http.factory.HttpClientFactory

/**
 * Spring configuration for Configurator HTTP client.
 */
@Configuration
class DirectConfiguratorClientConfiguration {

    @Bean(CONFIGURATOR_HTTP_CLIENT_BEAN_NAME)
    internal fun configuratorHttpClient(httpClientFactory: HttpClientFactory): HttpClient =
        httpClientFactory.create(CONFIGURATOR_CLIENT_NAME)

    /** Bean and client name constants. */
    companion object {
        const val CONFIGURATOR_HTTP_CLIENT_BEAN_NAME = "configuratorHttpClient"
        const val CONFIGURATOR_CLIENT_NAME = "configurator"
    }
}
