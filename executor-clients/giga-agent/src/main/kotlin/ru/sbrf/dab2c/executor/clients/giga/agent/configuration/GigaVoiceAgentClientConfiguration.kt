package ru.sbrf.dab2c.executor.clients.giga.agent.configuration

import io.ktor.client.HttpClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.audit.AuditedGigaVoiceAgentClientDecorator
import ru.sbrf.dab2c.executor.clients.giga.agent.impl.GigaVoiceAgentClientImpl
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceFunctionCallRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.monitoring.MonitoringGigaVoiceAgentClientDecorator
import ru.sbrf.dab2c.executor.clients.giga.agent.tracing.TracingGigaVoiceAgentClientDecorator
import ru.sbrf.dab2c.executor.clients.http.factory.HttpClientFactory
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade

/**
 * Spring configuration for GigaVoice Agent API client.
 */
@Configuration
class GigaVoiceAgentClientConfiguration {

    @Bean(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME)
    internal fun gigaVoiceAgentHttpClient(httpClientFactory: HttpClientFactory): HttpClient =
        httpClientFactory.create(GIGA_VOICE_AGENT_CLIENT_NAME)

    @Suppress("LongParameterList")
    @Bean
    internal fun gigaVoiceAgentClient(
        @Qualifier(GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME) httpClient: HttpClient,
        httpClientFactory: HttpClientFactory,
        settingsRequestBuilder: GigaVoiceSettingsRequestBuilder,
        functionCallRequestBuilder: GigaVoiceFunctionCallRequestBuilder,
        metricFactory: MetricFactory,
        agentInteractionAuditor: InteractionAuditor,
        tracingFacade: AefTracingFacade
    ): GigaVoiceAgentClient {

        val baseUrl = httpClientFactory.propertiesFor(GIGA_VOICE_AGENT_CLIENT_NAME).baseUrl

        val impl = GigaVoiceAgentClientImpl(
            httpClient, ObjectMappers.MAPPER, baseUrl, settingsRequestBuilder, functionCallRequestBuilder
        )

        val monitored = MonitoringGigaVoiceAgentClientDecorator(
            impl, metricFactory
        )

        val audited = AuditedGigaVoiceAgentClientDecorator(
            monitored, agentInteractionAuditor, ObjectMappers.MAPPER, baseUrl
        )

        return TracingGigaVoiceAgentClientDecorator(audited, tracingFacade, ObjectMappers.MAPPER)
    }

    internal companion object {
        internal const val GIGA_VOICE_AGENT_HTTP_CLIENT_BEAN_NAME = "gigaVoiceAgentClientHttpClient"
        internal const val GIGA_VOICE_AGENT_CLIENT_NAME = "giga-agent"
    }
}
