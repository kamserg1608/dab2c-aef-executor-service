package ru.sbrf.dab2c.executor.clients.efs.adapter.configuration

import io.ktor.client.HttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.http.factory.HttpClientFactory

/**
 * Spring configuration for EFS Adapter HTTP client.
 */
@Configuration
class EfsAdapterClientConfiguration {

    @Bean(EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME)
    internal fun efsAdapterHttpClient(httpClientFactory: HttpClientFactory): HttpClient =
        httpClientFactory.create(EFS_ADAPTER_CLIENT_NAME)

    /** Bean and client name constants. */
    companion object {
        const val EFS_ADAPTER_HTTP_CLIENT_BEAN_NAME = "efsAdapterHttpClient"
        const val EFS_ADAPTER_CLIENT_NAME = "efs-adapter"
    }
}
