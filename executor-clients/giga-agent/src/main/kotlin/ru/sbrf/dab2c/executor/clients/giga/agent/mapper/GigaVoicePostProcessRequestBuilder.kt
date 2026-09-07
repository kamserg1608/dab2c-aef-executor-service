package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessContextRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Builder for constructing PostProcessContextRequestSchema from the accumulated session state.
 */
@Component
class GigaVoicePostProcessRequestBuilder {

    /** Builds a post-processing request from the accumulated dialog context. */
    fun build(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        daSessionInfo: DaSessionInfo,
        contextData: DialogContext
    ): PostProcessContextRequestSchema {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(channel = context.channel)

        return PostProcessContextRequestSchema(
            conversationId = context.conversationId,
            eduId = context.eduId,
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            ),
            sessionInfo = DaSessionInfoApiMapper.toApiSessionInfo(daSessionInfo, context),
            userInfo = DaSessionInfoApiMapper.toApiUserInfo(daSessionInfo),
            context = ObjectMappers.MAPPER.convertValue(contextData.data)
        )
    }
}
