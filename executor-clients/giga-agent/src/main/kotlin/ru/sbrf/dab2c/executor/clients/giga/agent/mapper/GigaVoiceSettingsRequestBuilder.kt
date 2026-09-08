package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import com.fasterxml.jackson.module.kotlin.convertValue
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Builder for constructing GigaVoiceSettingsRequestSchema from proto inputs.
 */
@Component
class GigaVoiceSettingsRequestBuilder {

    /** Builds a settings request from proto inputs. */
    @Suppress("LongParameterList")
    fun build(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: Settings,
        daSessionInfo: DaSessionInfo,
        contextData: DialogContext
    ): GigaVoiceSettingsRequestSchema {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(
            channel = context.channel,
            platform = context.daPlatform
        )
        val settingsInput = GigaVoiceProtoToApiMapper.toApiSettingsInput(voiceSettings)

        return GigaVoiceSettingsRequestSchema(
            conversationId = context.conversationId,
            eduId = context.eduId,
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            ),
            settings = settingsInput,
            sessionInfo = DaSessionInfoApiMapper.toApiSessionInfo(daSessionInfo, context),
            userInfo = DaSessionInfoApiMapper.toApiUserInfo(daSessionInfo),
            context = ObjectMappers.MAPPER.convertValue(contextData.data)
        )
    }
}
