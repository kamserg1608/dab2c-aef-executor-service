package ru.sbrf.dab2c.executor.voice.factory.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.NoopChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ObservingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl

/**
 * Default implementation of ChunkProcessingServiceFactory.
 */
@Service
class ChunkProcessingServiceFactoryImpl(
    private val voiceExecutorConfigurationProperties: VoiceExecutorConfigurationProperties,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val typedSdsClient: TypedSdsClient
) : ChunkProcessingServiceFactory {

    override fun create(): ChunkProcessingService {
        val requestMetadata = GrpcMetadataContext.current()
        val isProxyMode = requestMetadata.proxy ?: voiceExecutorConfigurationProperties.proxyMode

        val delegate = if (isProxyMode) {
            NoopChunkProcessingServiceImpl()
        } else {
            createChunkProcessingServiceImpl()
        }

        return ObservingChunkProcessingServiceDelegate(delegate)
    }

    private fun createChunkProcessingServiceImpl(): ChunkProcessingService {
        val processingState = MutableStateFlow(ProcessingState())
        val callbackChannel = Channel<VoiceRequest>(capacity = Channel.BUFFERED)

        val functionCallService = FunctionCallServiceImpl(
            processingState,
            callbackChannel,
            gigaVoiceAgentClient
        )
        val settingsService = SettingsServiceImpl(
            processingState,
            callbackChannel,
            gigaVoiceAgentClient,
            configuratorClient,
            typedSdsClient,
            voiceExecutorConfigurationProperties
        )

        return ChunkProcessingServiceImpl(
            processingState,
            callbackChannel,
            settingsService,
            functionCallService
        )
    }
}
