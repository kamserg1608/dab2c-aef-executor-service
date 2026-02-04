package ru.sbrf.dab2c.executor.voice.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.GaugeMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata
import java.util.concurrent.atomic.AtomicInteger

/**
 * Decorator for monitoring voice chunk processing.
 * Tracks incoming and outgoing chunks with metrics and logging.
 */
class MonitoringChunksProcessingDecorator(
    private val delegate: ChunkProcessingService,
    private val monitoringServiceFactory: MonitoringServiceFactory
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }
    private val counters = mutableMapOf<String, CounterMetric>()
    private var activeConnectionsGauge: GaugeMetric? = null
    private var totalConnectionsCounter: CounterMetric? = null
    private var connectionDurationTimer: TimerSampleMetric? = null
    private val connectionsCounter = AtomicInteger(0)

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        logger.trace { "Start monitoring for incoming request flow" }

        var timerSample: TimerSampleMetric.TimerSample? = null

        val monitoredChunks = requestsChunks
            .onStart {
                ensureActiveConnectionsGaugeInitialized()
                ensureTotalConnectionsCounterInitialized()
                ensureConnectionDurationTimerInitialized()

                val newValue = connectionsCounter.incrementAndGet()
                activeConnectionsGauge?.set(newValue.toDouble())
                totalConnectionsCounter?.increment()

                // Начинаем замер времени
                timerSample = getOrCreateTimerSample()

                logger.info { "gRPC connection opened. Total active: $newValue" }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    logger.info { "gRPC connection closed gracefully" }
                } else {
                    logger.warn(cause) { "gRPC connection closed with error" }
                }
                val newValue = connectionsCounter.decrementAndGet()
                activeConnectionsGauge?.set(newValue.toDouble())

                timerSample?.stop()
            }
            .onEach {
                logger.info { "Request chunk: $it" }
                trackChunk(it, ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL)
            }
            .catch { e ->
                logger.error(e) { "Error on request stream: ${e.message}" }
                throw e
            }

        return delegate.processRequestChunks(monitoredChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        logger.trace { "Starting monitoring for the query output stream" }

        val monitoredChunks = responsesChunks
            .onEach { response ->
                logger.info { "Response chunk: ${response::class.simpleName}" }
                trackChunk(response, ExecutorVoiceMetric.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL)
                if (response is VoiceResponse.Output) {
                    val modelInfo = response.content as? ContentFromModel.AdditionalData

                    trackChunk(
                        response,
                        ExecutorVoiceMetric.GRPC_RESPONSE_TOTAL_TOKENS,
                        mapOf(
                            "model" to modelInfo?.data?.gigachatModelInfo?.name!!,
                            "version" to modelInfo?.data?.gigachatModelInfo?.version!!
                        ) // проверить
                    )
                }
            }
            .catch { e ->
                throw e
            }

        return delegate.processResponseChunks(monitoredChunks)
    }

    private suspend fun ensureConnectionDurationTimerInitialized() {
        if (connectionDurationTimer != null) return

        val platform = getPlatformHeader()
        val channel = getChannelHeader()

        connectionDurationTimer = monitoringServiceFactory.createTimerSample(
            ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION,
            platform = platform,
            channel = channel,
            tagsMap = emptyMap()
        )
    }

    private fun getOrCreateTimerSample(): TimerSampleMetric.TimerSample {
        return connectionDurationTimer?.start() ?: object : TimerSampleMetric.TimerSample {
            override fun stop() {} // fallback
        }
    }

    private suspend fun ensureActiveConnectionsGaugeInitialized() {
        if (activeConnectionsGauge != null) return

        val platform = getPlatformHeader()
        val channel = getChannelHeader()

        activeConnectionsGauge = monitoringServiceFactory.createGauge(
            ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE,
            platform = platform ,
            channel = channel,
            tagsMap = emptyMap()
        )
    }

    private suspend fun ensureTotalConnectionsCounterInitialized() {
        if (totalConnectionsCounter != null) return

        val platform = getPlatformHeader()
        val channel = getChannelHeader()

        totalConnectionsCounter = monitoringServiceFactory.createCounter(
            ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL,
            platform = platform,
            channel = channel,
            tagsMap = emptyMap()
        )
    }

    private suspend inline fun <reified T : Any> trackChunk(
        chunk: T,
        metricName: ExecutorVoiceMetric,
        additionalTags: Map<String, String> = emptyMap()
    ) {
        val className = chunk::class.simpleName!!
        val platform = getPlatformHeader()
        val channel = getChannelHeader()

        val tags = (mapOf(STREAM_CHUNK_TYPE_TAG to className) + additionalTags).toMap()
        "${metricName.name}|$platform|$channel|${tags.entries.joinToString("|")}"

        val counter = counters.getOrPut(className) {
            monitoringServiceFactory.createCounter(
                metricName,
                platform = platform,
                channel = channel,
                tagsMap = tags
            )
        }
        counter()
    }

    private suspend fun getPlatformHeader(): String {
        val metadata = currentRequestMetadata()
        return metadata.getHeader(RequestHeader.PLATFORM)
    }

    private suspend fun getChannelHeader(): String {
        val metadata = currentRequestMetadata()
        return metadata.getHeader(RequestHeader.CHANNEL)
    }

    /**
     * Chunk type key.
     */
    companion object {
        private const val STREAM_CHUNK_TYPE_TAG = "stream_chunk_type"
    }
}
