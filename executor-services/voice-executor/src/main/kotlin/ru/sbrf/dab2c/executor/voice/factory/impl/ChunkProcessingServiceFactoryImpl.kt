package ru.sbrf.dab2c.executor.voice.factory.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ContextServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.DialogAccumulatorDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.KapAnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.KapDialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.NoopChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.NoopDialogTurnPublisher
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
    private val typedSdsClient: TypedSdsClient,
    private val kapProducerClient: KapProducerClient
) : ChunkProcessingServiceFactory {

    override fun create(): ChunkProcessingService {
        val requestMetadata = GrpcMetadataContext.current()
        val isProxyMode = requestMetadata.proxy ?: voiceExecutorConfigurationProperties.proxyMode

        return if (isProxyMode) {
            createProxyModeService()
        } else {
            createFullModeService()
        }
    }

    private fun createProxyModeService(): ChunkProcessingService {
        val coreService = NoopChunkProcessingServiceImpl()
        return ObservingChunkProcessingServiceDelegate(
            DialogAccumulatorDelegate(coreService, NoopDialogTurnPublisher)
        )
    }

    private fun createFullModeService(): ChunkProcessingService {
        val processingState = MutableStateFlow<ProcessingState>(ProcessingState.AwaitingContext)
        val coreService = createChunkProcessingServiceImpl(processingState)
        val dialogTurnPublisher = KapDialogTurnPublisher(kapProducerClient, processingState)
        return ObservingChunkProcessingServiceDelegate(
            DialogAccumulatorDelegate(coreService, dialogTurnPublisher)
        )
    }

    private fun createChunkProcessingServiceImpl(
        processingState: MutableStateFlow<ProcessingState>
    ): ChunkProcessingService {
        val callbackChannel = Channel<VoiceRequest>(capacity = Channel.BUFFERED)
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val analyticsPublisher = KapAnalyticsPublisher(kapProducerClient, processingState)

        val contextService = ContextServiceImpl(processingState)
        val functionCallService = FunctionCallServiceImpl(
            processingState,
            callbackChannel,
            gigaVoiceAgentClient,
            sessionScope,
            analyticsPublisher
        )
        val settingsService = SettingsServiceImpl(
            processingState,
            callbackChannel,
            gigaVoiceAgentClient,
            configuratorClient,
            typedSdsClient,
            voiceExecutorConfigurationProperties,
            analyticsPublisher
        )

        return ChunkProcessingServiceImpl(
            processingState,
            callbackChannel,
            contextService,
            settingsService,
            functionCallService
        )
    }
}
