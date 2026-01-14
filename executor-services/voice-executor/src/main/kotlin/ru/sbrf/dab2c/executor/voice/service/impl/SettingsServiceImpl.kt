package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.LOADING_SETTINGS
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

/**
 * Default implementation of SettingsService.
 */
class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val configProperties: VoiceExecutorConfigurationProperties
) : SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: VoiceSettings) {
        processingState.value = processingState.value.copy(input = LOADING_SETTINGS)
        val processedSettings = calculateSettings(settings)
        callbackChannel.send(VoiceRequest.Settings(processedSettings))
        processingState.value = processingState.value.copy(input = SERVING)
    }

    private suspend fun calculateSettings(settings: VoiceSettings): VoiceSettings {
        val metadata = GrpcMetadataContext.current()
        val session = checkNotNull(metadata.session) { "Session header is required" }
        val token = checkNotNull(metadata.token) { "Token header is required" }
        val ufsCookie = checkNotNull(metadata.ufsCookie) { "UFS cookie could not be constructed" }

        logger.debug { "Calculating settings for session: $session" }

        val agentConfiguration = configuratorClient.getRestAgentConfig(
            agentName = configProperties.agentName,
            cookie = ufsCookie
        )
        logger.debug { "Fetched agent configuration: ${agentConfiguration.name}" }

        val (processedSettings, performers) = gigaVoiceAgentClient.getSettings(
            ufsSession = session,
            ufsToken = token,
            agentConfiguration = agentConfiguration,
            voiceSettings = settings,
            channel = configProperties.channel
        )
        logger.debug { "Received settings response with ${performers.functions.size} performers" }

        processingState.value = processingState.value.copy(functionRegistry = performers)

        return processedSettings
    }
}
