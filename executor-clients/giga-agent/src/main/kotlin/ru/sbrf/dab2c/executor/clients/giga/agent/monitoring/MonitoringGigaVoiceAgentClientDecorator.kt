package ru.sbrf.dab2c.executor.clients.giga.agent.monitoring

import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.monitoring.service.api.HttpCallDescriptor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.monitorHttpCall

/**
 * Decorator that records HTTP integration metrics for GigaVoice agent calls.
 */
class MonitoringGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val metricFactory: MetricFactory
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData,
    ): SettingsResult = metricFactory.monitorHttpCall(
        timerMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
        counterMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
        descriptor = HttpCallDescriptor(
            destinationService = GIGA_VOICE_AGENT,
            endpoint = SETTINGS_ENDPOINT
        )
    ) {
        delegate.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
    }

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): FunctionCallResult = metricFactory.monitorHttpCall(
        timerMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_DURATION_SECONDS,
        counterMetric = ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL,
        descriptor = HttpCallDescriptor(
            destinationService = AB_IVR,
            endpoint = FUNCTIONS_ENDPOINT
        )
    ) {
        delegate.executeFunctionCall(conversationId, agentConfiguration, functionCalling, contextData)
    }

    /** Destination service identifiers. */
    companion object {
        private const val GIGA_VOICE_AGENT = "gigavoice-agent"
        private const val AB_IVR = "ab-ivr"
    }
}
