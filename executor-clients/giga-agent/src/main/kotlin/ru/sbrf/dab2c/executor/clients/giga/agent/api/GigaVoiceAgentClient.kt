package ru.sbrf.dab2c.executor.clients.giga.agent.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Client for GigaVoice Agent API — resolves settings and executes backend function calls.
 */
interface GigaVoiceAgentClient {

    /** Resolves voice settings for the given agent and conversation. */
    suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        contextData: ContextData
    ): SettingsResult

    /** Executes a backend function call through the GigaVoice Agent API. */
    suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        contextData: ContextData
    ): FunctionCallResult

    /** Endpoint path constants. */
    companion object {
        const val SETTINGS_ENDPOINT = "/settings"
        const val FUNCTIONS_ENDPOINT = "/functions"
    }
}
