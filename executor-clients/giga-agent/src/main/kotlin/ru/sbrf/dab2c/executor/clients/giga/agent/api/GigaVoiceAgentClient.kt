package ru.sbrf.dab2c.executor.clients.giga.agent.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext

/** Client for GigaVoice Agent API. */
interface GigaVoiceAgentClient {

    /** Resolves voice settings for the given agent and conversation. */
    suspend fun getSettings(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        contextData: DialogContext
    ): SettingsResult

    /** Executes a backend function call through the GigaVoice Agent API. */
    suspend fun executeFunctionCall(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCalling,
        contextData: DialogContext
    ): FunctionCallResult

    /** Post-processes a finished conversation by its accumulated context. */
    suspend fun postProcess(
        conversationId: String,
        agentConfiguration: AgentConfiguration,
        contextData: DialogContext
    ): PostProcessResult

    /** Endpoint path constants. */
    companion object {
        const val SETTINGS_ENDPOINT = "/settings"
        const val FUNCTIONS_ENDPOINT = "/functions"
        const val POSTPROCESS_ENDPOINT = "/postprocess"
    }
}
