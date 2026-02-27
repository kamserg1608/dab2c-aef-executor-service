package ru.sbrf.dab2c.executor.voice.monitoring

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.monitoring.service.api.ConnectionHandle
import ru.sbrf.dab2c.executor.library.monitoring.service.api.ConnectionMetrics
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/**
 * Decorator that tracks gRPC connection lifecycle metrics:
 * active connections gauge, total connections counter, and connection duration timer.
 */
class MonitoringConnectionChunksProcessingDecorator(
    private val delegate: ChunkProcessingService,
    private val connectionMetrics: ConnectionMetrics
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> {
        var handle: ConnectionHandle? = null

        return delegate.processRequestChunks(
            requestsChunks
                .onStart {
                    handle = connectionMetrics.openConnection()
                    val opened = handle!!
                    logger.info { "gRPC connection opened. Active (${opened.connectionKey}): ${opened.activeCount}" }
                }
                .onCompletion { cause ->
                    if (cause == null) {
                        logger.info { "gRPC connection closed gracefully" }
                    } else {
                        logger.warn(cause) { "gRPC connection closed with error" }
                    }
                    handle?.close()
                    logger.debug { "Connection metrics finalized (${handle?.connectionKey})" }
                }
        )
    }

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> = delegate.processResponseChunks(responsesChunks)
}
