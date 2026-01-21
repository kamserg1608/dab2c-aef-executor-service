package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.toGigaAgentContext
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService

/**
 * Default implementation of SettingsService.
 */
@Suppress("LongParameterList")
class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val typedSdsClient: TypedSdsClient,
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

    @Suppress("LongMethod")
    private suspend fun calculateSettings(
        settings: VoiceSettings,
        contextData: ContextData
    ): VoiceSettings {
        val metadata = GrpcMetadataContext.current()

        logger.debug { "Calculating settings for session: ${metadata.session}" }

        val agentConfiguration = configuratorClient.getRestAgentConfig(
            agentName = configProperties.agentName,
            cookie = metadata.ufsCookie
        )
        logger.debug { "Fetched agent configuration: ${agentConfiguration.name}" }

        val sessionConfiguration = configuratorClient.getSessionConfig(cookie = metadata.ufsCookie)
        logger.debug { "Fetched session configuration: channel=${sessionConfiguration.channel}" }

        val daSessionInfo = typedSdsClient.readDaSessionInfo(sessionConfiguration.channel, metadata.ufsCookie)
        logger.debug { "Fetched DA session info for session: ${daSessionInfo.meta.sessionId}" }

        val conversationId = settings.voiceCallId

        val context = metadata.toGigaAgentContext(
            sessionConfiguration = sessionConfiguration,
            conversationId = conversationId,
            daSessionInfo = daSessionInfo
        )

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
            sessionConfiguration = sessionConfiguration,
            conversationId = conversationId,
            daSessionInfo = daSessionInfo,
            functionRegistry = settingsResult.performers
        )

        analyticsPublisher.publishAnalytics(settingsResult.analytics, context.daRequestId)

        return settingsResult.settings
    }
}
