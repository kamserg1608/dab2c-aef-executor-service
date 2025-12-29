package ru.sbrf.dab2c.executor.voice.service.impl

import GigaVoiceProtocol.GigaVoice
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.LOADING_SETTINGS
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.dab2c.executor.voice.util.extensions.toRequest

class SettingsServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<GigaVoice.GigaVoiceRequest>,
): SettingsService {

    private val logger = KotlinLogging.logger {}

    override suspend fun initSettingsCalculation(settings: GigaVoice.Settings) {
        processingState.value = processingState.value.copy(input = LOADING_SETTINGS)
        val processedSettings = calculateSettings(settings)
        callbackChannel.send(processedSettings.toRequest())
        processingState.value = processingState.value.copy(input = SERVING)
    }

    private suspend fun calculateSettings(settings: GigaVoice.Settings): GigaVoice.Settings {
        //TODO call client
        return settings
    }
}