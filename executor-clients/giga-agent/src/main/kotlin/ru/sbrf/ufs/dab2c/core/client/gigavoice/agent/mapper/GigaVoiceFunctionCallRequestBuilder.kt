package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.mapper

import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData

/**
 * Builder for constructing GigaVoiceFunctionsRequestSchema from domain models.
 */
@Component
class GigaVoiceFunctionCallRequestBuilder(
    private val mapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) {

    /**
     * Builds a function call request from domain models.
     */
    fun build(
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        channel: String
    ): GigaVoiceFunctionsRequestSchema {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(channel = channel)
        val apiFunctionCalling = mapper.toApiFunctionCalling(functionCalling)

        return GigaVoiceFunctionsRequestSchema(
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            ),
            functionCalling = apiFunctionCalling,
            sessionInfo = null,
            userInfo = null,
            context = null
        )
    }
}
