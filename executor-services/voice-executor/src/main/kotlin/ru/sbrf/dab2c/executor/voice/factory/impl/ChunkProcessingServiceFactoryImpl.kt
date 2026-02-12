package ru.sbrf.dab2c.executor.voice.factory.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.CallbackChannels
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.monitoring.MonitoringChunksProcessingDecorator
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ContextServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.DialogAccumulatorDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.KapAnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.KapDialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.LoggingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.NoopChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.NoopDialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl

/**
 * Default implementation of ChunkProcessingServiceFactory.
 */
@Service
class ChunkProcessingServiceFactoryImpl(
    private val voiceExecutorConfigurationProperties: VoiceExecutorConfigurationProperties,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val monitoringServiceFactory: MonitoringServiceFactory,
    private val kapProducerClient: KapProducerClient
) : ChunkProcessingServiceFactory {

    private val logger = KotlinLogging.logger {}

    override fun create(): ChunkProcessingService {
        val requestMetadata = GrpcMetadataContext.fromGrpcThread()
        val headerProxyMode = requestMetadata.getHeaderOrNull(RequestHeader.PROXY)?.toBoolean()
        val isProxyMode = headerProxyMode ?: voiceExecutorConfigurationProperties.proxyMode

        val modeSource = if (headerProxyMode != null) "grpc-header" else "config"
        logger.info { "Operating mode: ${if (isProxyMode) "PROXY" else "FULL"} (source=$modeSource)" }

        val observingService = if (isProxyMode) {
            createProxyModeService()
        } else {
            createFullModeService()
        }

        return MonitoringChunksProcessingDecorator(
            observingService,
            monitoringServiceFactory
        )
    }

    private fun createProxyModeService(): ChunkProcessingService {
        val coreService = NoopChunkProcessingServiceImpl()
        return LoggingChunkProcessingServiceDelegate(
            DialogAccumulatorDelegate(coreService, NoopDialogTurnPublisher)
        )
    }

    private fun createFullModeService(): ChunkProcessingService {
        val processingState = MutableStateFlow<ProcessingState>(ProcessingState.AwaitingContext)
        val coreService = createChunkProcessingServiceImpl(processingState)
        val dialogTurnPublisher = KapDialogTurnPublisher(kapProducerClient, processingState)
        return LoggingChunkProcessingServiceDelegate(
            DialogAccumulatorDelegate(coreService, dialogTurnPublisher)
        )
    }

    private fun createChunkProcessingServiceImpl(
        processingState: MutableStateFlow<ProcessingState>
    ): ChunkProcessingService {
        val callbackChannels = CallbackChannels(
            downstream = Channel(capacity = Channel.BUFFERED),
            upstream = Channel(capacity = Channel.BUFFERED)
        )
        val analyticsPublisher = KapAnalyticsPublisher(kapProducerClient, processingState)

        val contextService = ContextServiceImpl(processingState)
        val functionCallService = FunctionCallServiceImpl(
            processingState,
            callbackChannels,
            gigaVoiceAgentClient,
            analyticsPublisher
        )
        val settingsService = SettingsServiceImpl(
            processingState,
            callbackChannels,
            gigaVoiceAgentClient,
            configuratorClient,
            voiceExecutorConfigurationProperties,
            analyticsPublisher
        )

        return ChunkProcessingServiceImpl(
            processingState,
            callbackChannels,
            contextService,
            settingsService,
            functionCallService
        )
    }
}
