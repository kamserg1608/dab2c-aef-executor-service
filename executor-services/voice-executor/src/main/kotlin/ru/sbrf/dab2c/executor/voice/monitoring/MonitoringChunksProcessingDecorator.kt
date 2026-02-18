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
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.TimerSampleMetric
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata
import java.util.concurrent.ConcurrentHashMap
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

    @Suppress("LongMethod")
    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        logger.trace { "Start monitoring for incoming request flow" }

        var timerSample: TimerSampleMetric.TimerSample? = null
        var counter: AtomicInteger? = null
        var connectionKey: String? = null

        val monitoredChunks = requestsChunks
            .onStart {
                val platform = getPlatformHeader()
                val channel = getChannelHeader()
                connectionKey = "$platform:$channel"

                counter = activeConnectionCounters.computeIfAbsent(connectionKey!!) { AtomicInteger(0) }

                monitoringServiceFactory.createGauge(
                    ExecutorVoiceMetric.GRPC_CONNECTIONS_ACTIVE,
                    platform = platform,
                    channel = channel,
                    tagsMap = emptyMap(),
                    stateObject = counter!!
                ) { it.get().toDouble() }
                counter!!.incrementAndGet()

                monitoringServiceFactory.createCounter(
                    ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL,
                    platform = platform,
                    channel = channel,
                    tagsMap = emptyMap()
                ).increment()

                timerSample = monitoringServiceFactory.createTimerSample(
                    ExecutorVoiceMetric.GRPC_CONNECTIONS_DURATION,
                    platform = platform,
                    channel = channel,
                    tagsMap = emptyMap()
                ).start()

                logger.info { "gRPC connection opened. Active connections ($connectionKey): ${counter!!.get()}" }
            }
            .onCompletion { cause ->
                if (cause == null) {
                    logger.info { "gRPC connection closed gracefully" }
                } else {
                    logger.warn(cause) { "gRPC connection closed with error" }
                }

                val count = counter?.decrementAndGet()
                if (count != null && count <= 0) {
                    connectionKey?.let { activeConnectionCounters.remove(it) }
                }
                timerSample?.stop()
            }
            .onEach {
                trackChunk(it, ExecutorVoiceMetric.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL)
            }
            .catch { e ->
                logger.error(e) { "Error on request stream: ${e.message}" }
                throw e
            }

        return delegate.processRequestChunks(monitoredChunks)
    }

    @Suppress("LongMethod")
    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        logger.trace { "Starting monitoring for the query output stream" }

        var firstTranscriptionReceived = false
        var timeToFirstTranscriptionSample: TimerSampleMetric.TimerSample? = null

        val monitoredChunks = responsesChunks
            .onStart {
                val platform = getPlatformHeader()
                val channel = getChannelHeader()

                timeToFirstTranscriptionSample = monitoringServiceFactory.createTimerSample(
                    ExecutorVoiceMetric.GRPC_CONNECTIONS_TTFB_SECONDS,
                    platform = platform,
                    channel = channel,
                    tagsMap = emptyMap()
                ).start()
            }
            .onEach { response ->
                trackChunk(response, ExecutorVoiceMetric.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL)

                if (!firstTranscriptionReceived && response is VoiceResponse.InputTranscription) {
                    timeToFirstTranscriptionSample?.stop()
                    firstTranscriptionReceived = true
                    logger.debug { "First InputTranscription received, time measured." }
                }

                if (response is VoiceResponse.Output) {
                    val modelInfo = response.content as? ContentFromModel.AdditionalData

                    trackChunk(
                        response,
                        ExecutorVoiceMetric.GRPC_RESPONSE_TOTAL_TOKENS,
                        mapOf(
                            "model" to (modelInfo?.data?.gigachatModelInfo?.name ?: UNKNOWN),
                            "version" to (modelInfo?.data?.gigachatModelInfo?.version ?: UNKNOWN)
                        )
                    )
                }
            }
            .catch { e ->
                throw e
            }

        return delegate.processResponseChunks(monitoredChunks)
    }

    private suspend inline fun <reified T : Any> trackChunk(
        chunk: T,
        metricName: ExecutorVoiceMetric,
        additionalTags: Map<String, String> = emptyMap()
    ) {
        val className = chunk::class.simpleName!!
        val platform = getPlatformHeader()
        val channel = getChannelHeader()
        val tags = mapOf(STREAM_CHUNK_TYPE_TAG to className) + additionalTags

        monitoringServiceFactory.createCounter(
            metricName,
            platform = platform,
            channel = channel,
            tagsMap = tags
        ).increment()
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
        private const val UNKNOWN = "unknown"

        private val activeConnectionCounters = ConcurrentHashMap<String, AtomicInteger>()
    }
}
