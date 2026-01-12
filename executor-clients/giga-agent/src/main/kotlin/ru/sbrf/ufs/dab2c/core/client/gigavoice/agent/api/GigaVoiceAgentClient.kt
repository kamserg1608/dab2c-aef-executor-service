package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema

/**
 * Client interface for GigaVoice Agent API.
 */
interface GigaVoiceAgentClient {

    /**
     * Get full configuration and function registry for a GigaVoice session
     *
     * @param ufsSession UFS session identifier
     * @param ufsToken UFS session token
     * @param request Configuration, Settings and SessionInfo objects to get actual settings
     * @return Response containing full settings and function registry
     */
    suspend fun getSettings(
        ufsSession: String,
        ufsToken: String,
        request: GigaVoiceSettingsRequestSchema
    ): GigaVoiceSettingsResponseSchema

    /**
     * Execute a function call on the AB IVR side
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
