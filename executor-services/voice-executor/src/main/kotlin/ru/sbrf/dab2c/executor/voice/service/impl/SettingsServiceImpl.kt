package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.readDaSessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.LOADING_SETTINGS
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.toGigaAgentContext
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService

/**
 * Default implementation of SettingsService.
 */
class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val typedSdsClient: TypedSdsClient,
    private val configProperties: VoiceExecutorConfigurationProperties
) : SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: VoiceSettings) {
        processingState.value = processingState.value.copy(input = LOADING_SETTINGS)
        val processedSettings = calculateSettings(settings)
        callbackChannel.send(VoiceRequest.Settings(processedSettings))
        processingState.value = processingState.value.copy(input = SERVING)
    }

    @Suppress("LongMethod")
    private suspend fun calculateSettings(settings: VoiceSettings): VoiceSettings {
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
        processingState.value = processingState.value.copy(
            agentConfiguration = agentConfiguration,
            sessionConfiguration = sessionConfiguration,
            conversationId = conversationId,
            daSessionInfo = daSessionInfo
        )

        val context = metadata.toGigaAgentContext(
            sessionConfiguration = sessionConfiguration,
            conversationId = conversationId,
            daSessionInfo = daSessionInfo
        )

        val (processedSettings, performers) = gigaVoiceAgentClient.getSettings(
            context = context,
            agentConfiguration = agentConfiguration,
            voiceSettings = settings,
            daSessionInfo = daSessionInfo
        )
        logger.debug { "Received settings response with ${performers.functions.size} performers" }

        processingState.value = processingState.value.copy(functionRegistry = performers)

        return processedSettings
    }
}
