@file:Suppress("LongParameterList")

package ru.sbrf.dab2c.executor.clients.giga.agent.api

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Client interface for GigaVoice Agent API.
 */
interface GigaVoiceAgentClient {

    /**
     * Get full configuration and function registry for a GigaVoice session.
     */
    suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        channel: String,
        conversationId: String,
        eduId: String
    ): Pair<VoiceSettings, FunctionPerformers>

    /**
     * Execute a function call on the AB IVR side.
     */
    suspend fun executeFunctionCall(
        ufsSession: String,
        ufsToken: String,
        functionCalling: FunctionCallingData,
        agentConfiguration: AgentConfiguration,
        channel: String,
        conversationId: String,
        eduId: String
    ): FunctionResultData
}
