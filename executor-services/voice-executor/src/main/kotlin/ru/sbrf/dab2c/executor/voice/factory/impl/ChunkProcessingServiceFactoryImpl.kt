package ru.sbrf.dab2c.executor.voice.factory.impl

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
import ru.sbrf.dab2c.executor.voice.mapper.FunctionCallSettingsProtoMapper
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
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
    private val timeProvider: TimeProvider,
    private val functionCallSettingsProtoMapper: FunctionCallSettingsProtoMapper
) : ChunkProcessingServiceFactory {

    private val connectionMetrics = ConnectionMetrics(
        metricFactory,
        activeMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE,
        totalMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL,
        durationMetric = ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION_SECONDS
    )

    override fun create(): ChunkProcessingService {
        val session = VoiceSession()
        val coreService = createCoreService(session)
        val dialogTurnPublisher = KapDialogTurnPublisher(kapProducerClient, session)
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

    private fun createCoreService(session: VoiceSession): ChunkProcessingService {
        val analyticsPublisher = KapAnalyticsPublisher(kapProducerClient, session)
        return ChunkProcessingServiceImpl(
            session,
            contextService = ContextServiceImpl(session),
            settingsService = SettingsServiceImpl(
                session,
                gigaVoiceAgentClient,
                configuratorClient,
                voiceExecutorConfigurationProperties,
                analyticsPublisher,
                functionCallSettingsProtoMapper
            ),
            functionCallService = FunctionCallServiceImpl(
                session,
                gigaVoiceAgentClient,
                analyticsPublisher
            )
        )
    }
}
