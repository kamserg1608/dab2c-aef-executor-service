package ru.sbrf.dab2c.executor.voice.factory.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.NoopChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ObservingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

@Service
class ChunkProcessingServiceFactoryImpl(
    private val voiceExecutorConfigurationProperties: VoiceExecutorConfigurationProperties,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient
) : ChunkProcessingServiceFactory {

    override fun create(): ChunkProcessingService {
        val delegate = if (voiceExecutorConfigurationProperties.proxyMode) {
            NoopChunkProcessingServiceImpl()
        } else {
            createChunkProcessingServiceImpl()
        }

        return ObservingChunkProcessingServiceDelegate(delegate)
    }

    private fun createChunkProcessingServiceImpl(): ChunkProcessingService {
        val processingState = MutableStateFlow(ProcessingState())
        val callbackChannel = Channel<VoiceRequest>(capacity = Channel.BUFFERED)

        val functionCallService = FunctionCallServiceImpl(processingState, callbackChannel, gigaVoiceAgentClient)
        val settingsService = SettingsServiceImpl(processingState, callbackChannel, gigaVoiceAgentClient)

        return ChunkProcessingServiceImpl(
            processingState,
            callbackChannel,
            settingsService,
            functionCallService
        )
    }
}
