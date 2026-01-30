package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
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
 * Non-audio chunks are logged at DEBUG level with full content.
 * Audio chunks are logged at TRACE level with binary content omitted.
 */
class LoggingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }
    private val sessionStartTime = System.currentTimeMillis()

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        val observedChunks = requestsChunks
            .onStart { logSessionStart() }
            .onEach { request -> logRequest(request) }
            .onCompletion { error -> logSessionEnd(error) }
        return delegate.processRequestChunks(observedChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val observedChunks = responsesChunks
            .onEach { response -> logResponse(response) }
        return delegate.processResponseChunks(observedChunks)
    }

    private fun logRequest(request: VoiceRequest) {
        if (request is VoiceRequest.Audio) {
            logger.trace { "$REQUEST_PREFIX${request.copy(content = request.content.copy(audioChunk = null))}" }
        } else {
            logger.debug { "$REQUEST_PREFIX$request" }
        }
    }

    @Suppress("MaximumLineLength", "MaxLineLength")
    private fun logResponse(response: VoiceResponse) {
        if (response is VoiceResponse.Output && response.content is ContentFromModel.Audio) {
            val content = response.content as ContentFromModel.Audio
            logger.trace {
                "${RESPONSE_PREFIX}Output(content=Audio(audio=AudioOutput(audioChunk=<omitted>, audioDuration=${content.audio.audioDuration}, isFinal=${content.audio.isFinal})))"
            }
        } else {
            logger.debug { "$RESPONSE_PREFIX$response" }
        }
    }

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

    private companion object {
        private const val REQUEST_PREFIX = "Request: "
        private const val RESPONSE_PREFIX = "Response: "
    }
}
