package ru.sbrf.dab2c.executor.voice.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.CounterMetric
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MonitoringServiceFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorMetrics
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

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

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        logger.trace { "Start monitoring for incoming request flow" }

        val monitoredChunks = requestsChunks
            .onEach { trackChunk(it, ExecutorMetrics.GRPC_INCOMING_FROM_INITIATOR_CHUNKS_TOTAL) }
            .catch { e ->
                logger.error(e) { "Error on request stream: ${e.message}" }
                throw e
            }

        return delegate.processRequestChunks(monitoredChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        logger.trace { "Starting monitoring for the query output stream" }

        val monitoredChunks = responsesChunks
            .onEach { trackChunk(it, ExecutorMetrics.GRPC_OUTGOING_FROM_INITIATOR_CHUNKS_TOTAL) }
            .catch { e ->
                throw e
            }

        return delegate.processResponseChunks(monitoredChunks)
    }

    private suspend inline fun <reified T : Any> trackChunk(chunk: T, metricName: ExecutorMetrics) {
        val className = chunk::class.simpleName!!

        val counter = counters.getOrPut(className) {
            monitoringServiceFactory.createCounter(
                metricName,
                getPlatformHeader(),
                getChannelHeader(),
                tagsMap = mapOf(STREAM_CHUNK_TYPE_TAG to className)
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
