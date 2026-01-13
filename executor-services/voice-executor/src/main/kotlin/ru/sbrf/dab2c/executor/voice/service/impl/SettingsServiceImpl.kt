package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.LOADING_SETTINGS
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient
) : SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: VoiceSettings) {
        processingState.value = processingState.value.copy(input = LOADING_SETTINGS)
        val processedSettings = calculateSettings(settings)
        callbackChannel.send(VoiceRequest.Settings(processedSettings))
        processingState.value = processingState.value.copy(input = SERVING)
    }

    private suspend fun calculateSettings(settings: VoiceSettings): VoiceSettings {
        // TODO: call gigaVoiceAgentClient.getSettings()
        return settings
    }
}
