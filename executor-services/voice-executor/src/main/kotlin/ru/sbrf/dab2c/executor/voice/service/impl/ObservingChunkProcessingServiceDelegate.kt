package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.logging.IntegrationLogger
import ru.sbrf.dab2c.executor.voice.model.RequestHeader
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

/**
 * ChunkProcessingService decorator that logs request/response chunks and session lifecycle.
 * Skips audio chunk logging to avoid polluting logs with high-frequency data.
 */
class ObservingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }
    private val sessionStartTime = System.currentTimeMillis()

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        val observedChunks = requestsChunks
            .onStart { logSessionStart() }
            .onEach { request ->
                if (request !is VoiceRequest.Audio) {
                    logger.debug { "Received request: type=${request::class.simpleName}" }
                }
            }
            .catch { e -> logger.error(e) { "Error on request stream: ${e.message}" }.also { throw e } }
            .onCompletion { error ->
                logSessionEnd(error)
            }
        return delegate.processRequestChunks(observedChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val observedChunks = responsesChunks
            .catch { e -> logger.error(e) { "Error on response stream: ${e.message}" }.also { throw e } }
            .onEach { response ->
                if (!isAudioResponse(response)) {
                    logger.debug { "Received response: type=${response::class.simpleName}" }
                }
            }
        return delegate.processResponseChunks(observedChunks)
    }

    private fun isAudioResponse(response: VoiceResponse): Boolean =
        response is VoiceResponse.Output && response.content is ContentFromModel.Audio

    private suspend fun logSessionStart() {
        val metadataString = buildMetadataString()
        IntegrationLogger.logGrpcEvent(
            message = "gRPC session started",
            rqMessage = metadataString
        )
    }

    private fun logSessionEnd(error: Throwable?) {
        val executionTime = System.currentTimeMillis() - sessionStartTime
        IntegrationLogger.logGrpcEvent(
            message = if (error == null) "gRPC session completed" else "gRPC session failed",
            executionTime = executionTime,
            error = error
        )
    }

    private suspend fun buildMetadataString(): String {
        val metadata = currentRequestMetadata()
        return buildString {
            append("channel=").append(metadata.getHeaderOrNull(RequestHeader.CHANNEL) ?: "")
            append(", platform=").append(metadata.getHeaderOrNull(RequestHeader.PLATFORM) ?: "")
            append(", x-request-id=").append(metadata.getHeaderOrNull(RequestHeader.X_REQUEST_ID) ?: "")
        }
    }
}
