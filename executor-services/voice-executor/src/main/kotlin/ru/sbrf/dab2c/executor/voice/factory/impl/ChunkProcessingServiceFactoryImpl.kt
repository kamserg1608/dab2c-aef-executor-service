package ru.sbrf.dab2c.executor.voice.factory.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.ConnectionMetrics
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.time.TimeProvider
import ru.sbrf.dab2c.executor.voice.config.properties.VoiceExecutorConfigurationProperties
import ru.sbrf.dab2c.executor.voice.factory.api.ChunkProcessingServiceFactory
import ru.sbrf.dab2c.executor.voice.model.CallbackChannels
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.monitoring.MonitoringChunksProcessingDecorator
import ru.sbrf.dab2c.executor.voice.monitoring.MonitoringConnectionChunksProcessingDecorator
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.impl.ChunkProcessingServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.ContextServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.DialogAccumulatorDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.KapAnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.KapDialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.LoggingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl

/** Default implementation of ChunkProcessingServiceFactory. */
@Suppress("LongParameterList")
@Service
class ChunkProcessingServiceFactoryImpl(
    private val voiceExecutorConfigurationProperties: VoiceExecutorConfigurationProperties,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val configuratorClient: ConfiguratorClient,
    private val metricFactory: MetricFactory,
    private val kapProducerClient: KapProducerClient,
    private val externalInteractionAuditor: InteractionAuditor,
    private val timeProvider: TimeProvider
) : ChunkProcessingServiceFactory {

    private val connectionMetrics = ConnectionMetrics(
        metricFactory,
        activeMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE,
        totalMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL,
        durationMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION_SECONDS
    )

    override fun create(): ChunkProcessingService {
        val processingState = MutableStateFlow<ProcessingState>(ProcessingState.AwaitingContext)
        val coreService = createCoreService(processingState)
        val dialogTurnPublisher = KapDialogTurnPublisher(kapProducerClient, processingState)
        return MonitoringConnectionChunksProcessingDecorator(
            MonitoringChunksProcessingDecorator(
                LoggingChunkProcessingServiceDelegate(
                    DialogAccumulatorDelegate(
                        coreService, dialogTurnPublisher, externalInteractionAuditor, timeProvider
                    )
                ),
                metricFactory
            ),
            connectionMetrics
        )
    }

    private fun createCoreService(
        processingState: MutableStateFlow<ProcessingState>
    ): ChunkProcessingService {
        val callbackChannels = CallbackChannels(
            downstream = Channel(capacity = Channel.BUFFERED),
            upstream = Channel(capacity = Channel.BUFFERED)
        )
        val analyticsPublisher = KapAnalyticsPublisher(kapProducerClient, processingState)
        return ChunkProcessingServiceImpl(
            processingState,
            callbackChannels,
            contextService = ContextServiceImpl(processingState),
            settingsService = SettingsServiceImpl(
                processingState,
                callbackChannels,
                gigaVoiceAgentClient,
                configuratorClient,
                voiceExecutorConfigurationProperties,
                analyticsPublisher
            ),
            functionCallService = FunctionCallServiceImpl(
                processingState,
                callbackChannels,
                gigaVoiceAgentClient,
                analyticsPublisher
            )
        )
    }
}
