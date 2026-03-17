package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

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
    @Suppress("LongParameterList")
    fun build(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        functionCalling: FunctionCallingData,
        daSessionInfo: DaSessionInfo,
        contextData: ContextData,
        headers: Headers
    ): GigaVoiceFunctionsRequestSchema {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(channel = context.channel)
        val apiFunctionCalling = mapper.toApiFunctionCalling(functionCalling)

        return GigaVoiceFunctionsRequestSchema(
            conversationId = context.conversationId,
            eduId = context.eduId,
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            ),
            functionCalling = apiFunctionCalling,
            sessionInfo = DaSessionInfoApiMapper.toApiSessionInfo(daSessionInfo, headers),
            userInfo = DaSessionInfoApiMapper.toApiUserInfo(daSessionInfo),
            context = parseContextJson(contextData.content)
        )
    }

    private fun parseContextJson(json: String): Map<String, Any> =
        if (json.isEmpty()) emptyMap() else ObjectMappers.MAPPER.readValue(json)
}
