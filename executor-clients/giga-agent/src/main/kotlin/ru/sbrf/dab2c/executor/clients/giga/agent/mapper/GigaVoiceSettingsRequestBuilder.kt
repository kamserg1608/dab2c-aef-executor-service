package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionConfig
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Builder for constructing GigaVoiceSettingsRequestSchema from domain models.
 */
@Component
class GigaVoiceSettingsRequestBuilder(
    private val settingsMapper: GigaVoiceSettingsMapper = GigaVoiceSettingsMapper
) {

    /**
     * Builds a settings request from domain models.
     */
    fun build(
        context: GigaAgentRequestContext,
        agentConfiguration: AgentConfiguration,
        voiceSettings: VoiceSettings,
        daSessionInfo: DaSessionInfo
    ): GigaVoiceSettingsRequestSchema {
        val agentConfig = GigaVoiceAgentConfigMapper.toApiAgentConfig(agentConfiguration)
        val sessionConfig = SessionConfig(channel = context.channel)
        val settingsInput = settingsMapper.toApiSettingsInput(voiceSettings)

        return GigaVoiceSettingsRequestSchema(
            conversationId = context.conversationId,
            eduId = context.eduId,
            config = ACLConfig(
                agentConfig = agentConfig,
                sessionConfig = sessionConfig
            ),
            settings = settingsInput,
            sessionInfo = DaSessionInfoApiMapper.toApiSessionInfo(daSessionInfo),
            userInfo = DaSessionInfoApiMapper.toApiUserInfo(daSessionInfo),
            context = null
        )
    }
}
