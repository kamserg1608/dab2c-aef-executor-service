package ru.sbrf.dab2c.executor.voice.factory.impl

import GigaVoiceProtocol.GigaVoice
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ObservingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl

@Service
class ChunkProcessingServiceFactoryImpl: ChunkProcessingServiceFactory {

    override fun create(): ChunkProcessingService {

        val processingState = MutableStateFlow(ProcessingState())
        val callbackChannel = Channel<GigaVoice.GigaVoiceRequest>(capacity = Channel.BUFFERED)

        val functionCallService = FunctionCallServiceImpl(callbackChannel)
        val settingsService = SettingsServiceImpl(processingState, callbackChannel)

        return ChunkProcessingServiceImpl(
                processingState,
                callbackChannel,
                settingsService,
                functionCallService
            )
            .let { ObservingChunkProcessingServiceDelegate(it) }
    }
}