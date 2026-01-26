package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.model.toGigaAgentContext
import ru.sbrf.dab2c.executor.voice.model.ufsCookie
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService

/**
 * Default implementation of SettingsService.
 */
class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val configProperties: VoiceExecutorConfigurationProperties,
    private val analyticsPublisher: AnalyticsPublisher
) : SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: VoiceSettings) {
        val currentState = processingState.value
        check(currentState is ProcessingState.AwaitingSettings) {
            "Expected AwaitingSettings state, but was ${currentState::class.simpleName}"
        }

        processingState.value = ProcessingState.LoadingSettings(currentState.contextData)
        val processedSettings = calculateSettings(settings, currentState.contextData)
        callbackChannel.send(VoiceRequest.Settings(processedSettings))
    }

    private suspend fun calculateSettings(
        settings: VoiceSettings,
        contextData: ContextData
    ): VoiceSettings {
        val metadata = GrpcMetadataContext.current()

        logger.debug { "Calculating settings for session: ${metadata.getHeader(RequestHeader.SESSION)}" }

        val daSessionInfo = metadata.daSessionInfo

        val agentConfiguration = configuratorClient.getRestAgentConfig(configProperties.agentName, metadata.ufsCookie)

        logger.debug { "Fetched agent configuration: ${agentConfiguration.name}" }
        logger.debug { "Fetched DA session info for session: ${daSessionInfo.meta.sessionId}" }

        val conversationId = settings.voiceCallId

        val context = metadata.toGigaAgentContext(conversationId)

        val settingsResult = gigaVoiceAgentClient.getSettings(
            context = context,
            agentConfiguration = agentConfiguration,
            voiceSettings = settings,
            daSessionInfo = daSessionInfo,
            contextData = contextData
        )
        logger.debug { "Received settings response with ${settingsResult.performers.functions.size} performers" }

        processingState.value = ProcessingState.Serving(
            contextData = contextData,
            agentConfiguration = agentConfiguration,
            conversationId = conversationId,
            functionRegistry = settingsResult.performers
        )

        analyticsPublisher.publishAnalytics(settingsResult.analytics, context.daRequestId)

        return settingsResult.settings
    }
}
