package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Client interface for GigaVoice Agent API.
 */
interface GigaVoiceAgentClient {

    /**
     * Get full configuration and function registry for a GigaVoice session.
     *
     * @param ufsSession UFS session identifier
     * @param ufsToken UFS session token
     * @param agentConfiguration Agent configuration from EFS adapter
     * @param voiceSettings Voice settings from the client
     * @param channel Channel identifier
     * @return Pair of processed settings and function performers
     */
    suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        channel: String
    ): Pair<VoiceSettings, FunctionPerformers>

    /**
     * Execute a function call on the AB IVR side.
     *
     * @param ufsSession UFS session identifier
     * @param ufsToken UFS session token
     * @param request Agent configuration, session info and function call to execute
     * @return Function execution result
     */
    suspend fun executeFunctionCall(
        ufsSession: String,
        ufsToken: String,
        request: GigaVoiceFunctionsRequestSchema
    ): GigaVoiceFunctionsResponseSchema
}
