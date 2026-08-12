@file:Suppress("StringLiteralDuplication")

package ru.sbrf.dab2c.executor.clients.giga.agent.tracing

import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.FUNCTIONS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient.Companion.SETTINGS_ENDPOINT
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracing

/** Wraps [GigaVoiceAgentClient] HTTP calls in `output_request` spans with real bodies. */
class TracingGigaVoiceAgentClientDecorator(
    private val delegate: GigaVoiceAgentClient,
    private val aefTracing: AefHttpOutgoingRequestTracing,
) : GigaVoiceAgentClient {

    override suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: Context
    ): SettingsResult = aefTracing.trace(
        spanName = "agent $SETTINGS_ENDPOINT",
        path = SETTINGS_ENDPOINT,
        request = mapOf(
            "conversationId" to conversationId,
            "agentConfiguration" to agentConfiguration,
            "voiceSettings" to voiceSettings,
            "contextData" to contextData
        )
    ) {
        delegate.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
    }

    override suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: Context
    ): FunctionCallResult = aefTracing.trace(
        spanName = "agent $FUNCTIONS_ENDPOINT",
        path = FUNCTIONS_ENDPOINT,
        request = mapOf(
            "conversationId" to conversationId,
            "agentConfiguration" to agentConfiguration,
            "functionCalling" to functionCalling,
            "contextData" to contextData
        )
    ) {
        delegate.executeFunctionCall(conversationId, agentConfiguration, functionCalling, contextData)
    }
}
