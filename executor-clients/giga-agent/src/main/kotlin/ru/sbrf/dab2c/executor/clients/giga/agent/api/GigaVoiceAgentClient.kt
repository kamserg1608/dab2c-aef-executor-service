package ru.sbrf.dab2c.executor.clients.giga.agent.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration

/**
 * Client for GigaVoice Agent API — resolves settings and executes backend function calls.
 */
interface GigaVoiceAgentClient {

    /** Resolves voice settings for the given agent and conversation. */
    suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: Context
    ): SettingsResult

    /** Executes a backend function call through the GigaVoice Agent API. */
    suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: Context
    ): FunctionCallResult

    /** Endpoint path constants. */
    companion object {
        const val SETTINGS_ENDPOINT = "/settings"
        const val FUNCTIONS_ENDPOINT = "/functions"
    }
}
