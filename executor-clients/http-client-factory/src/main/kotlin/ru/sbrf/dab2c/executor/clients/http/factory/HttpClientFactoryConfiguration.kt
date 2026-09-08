package ru.sbrf.dab2c.executor.clients.http.factory

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration exposing the shared [HttpClientFactory].
 */
@Configuration
@EnableConfigurationProperties(HttpClientsProperties::class)
class HttpClientFactoryConfiguration {

    @Bean
    internal fun httpClientFactory(
        properties: HttpClientsProperties,
        @Value("\${aef.agent.agentId:}") agentId: String
    ): HttpClientFactory = HttpClientFactory(properties, agentId)
}
