package ru.sbrf.dab2c.executor.voice.factory.impl

import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.clients.configurator.api.DirectConfiguratorClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.ConfiguratorClient
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.iag.api.IagFunctionClient
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.monitoring.service.api.ConnectionMetrics
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.time.TimeProvider
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade
import ru.sbrf.dab2c.executor.voice.audit.DialogTurnAuditor
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
import ru.sbrf.dab2c.executor.voice.service.impl.FunctionCallServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.KapAnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.KapDialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.LoggingChunkProcessingServiceDelegate
import ru.sbrf.dab2c.executor.voice.service.impl.SettingsServiceImpl
import ru.sbrf.dab2c.executor.voice.service.impl.VoiceSessionObserverDelegate
import ru.sbrf.dab2c.executor.voice.session.observer.CompositeVoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.tracing.DialogTracingPublisher

/** Default implementation of ChunkProcessingServiceFactory. */
@Suppress("LongParameterList")
@Service
class ChunkProcessingServiceFactoryImpl(
    private val voiceExecutorConfigurationProperties: VoiceExecutorConfigurationProperties,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient,
    private val directConfiguratorClient: DirectConfiguratorClient,
    private val configuratorClient: ConfiguratorClient,
    private val metricFactory: MetricFactory,
    private val kapProducerClient: KapProducerClient,
    private val iagFunctionClient: IagFunctionClient,
    private val externalInteractionAuditor: InteractionAuditor,
    private val timeProvider: TimeProvider,
    private val tracingFacade: AefTracingFacade,
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
        val sessionObserver = CompositeVoiceSessionObserver(buildSessionObservers(session))
        return MonitoringConnectionChunksProcessingDecorator(
            MonitoringChunksProcessingDecorator(
                LoggingChunkProcessingServiceDelegate(
                    VoiceSessionObserverDelegate(coreService, sessionObserver, timeProvider)
                ),
                metricFactory
            ),
            connectionMetrics
        )
    }

    private fun buildSessionObservers(session: VoiceSession): List<VoiceSessionObserver> = listOf(
        KapDialogTurnPublisher(kapProducerClient, session),
        DialogTurnAuditor(externalInteractionAuditor),
        DialogTracingPublisher(tracingFacade),
    )

    private fun createCoreService(session: VoiceSession): ChunkProcessingService {
        val analyticsPublisher = KapAnalyticsPublisher(kapProducerClient, session)
        return ChunkProcessingServiceImpl(
            session,
            contextService = ContextServiceImpl(session),
            settingsService = SettingsServiceImpl(
                session,
                gigaVoiceAgentClient,
                directConfiguratorClient,
                configuratorClient,
                voiceExecutorConfigurationProperties,
                analyticsPublisher,
                functionCallSettingsProtoMapper
            ),
            functionCallService = FunctionCallServiceImpl(
                session,
                gigaVoiceAgentClient,
                iagFunctionClient,
                configuratorClient,
                analyticsPublisher
            )
        )
    }
}
