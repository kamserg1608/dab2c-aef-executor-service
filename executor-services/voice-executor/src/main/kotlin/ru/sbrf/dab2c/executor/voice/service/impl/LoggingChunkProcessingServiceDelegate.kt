package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.logging.IntegrationLogger
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/** Decorator that adds request/response logging to chunk processing. */
class LoggingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }
    private val sessionStartTime = System.currentTimeMillis()

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        val observedChunks = requestsChunks
            .onStart { logSessionStart() }
            .onEach { request -> logRequest(DIRECTION_IN, request) }
        return delegate.processRequestChunks(observedChunks)
            .onEach { request -> logRequest(DIRECTION_OUT, request) }
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val observedChunks = responsesChunks
            .onEach { response -> logResponse(DIRECTION_IN, response) }
        return delegate.processResponseChunks(observedChunks)
            .onEach { response -> logResponse(DIRECTION_OUT, response) }
            .onCompletion { error -> logSessionEnd(error) }
    }

    private fun logRequest(direction: String, request: VoiceRequest) {
        if (request is VoiceRequest.Audio) {
            logger.debug {
                "$direction$REQUEST_PREFIX${request.copy(content = request.content.copy(audioChunk = null))}"
            }
        } else {
            logger.debug { "$direction$REQUEST_PREFIX$request" }
        }
    }

    private fun logResponse(direction: String, response: VoiceResponse) {
        if (response is VoiceResponse.Output && response.content is ContentFromModel.Audio) {
            val content = response.content as ContentFromModel.Audio
            val sanitized = response.copy(
                content = content.copy(audio = content.audio.copy(audioChunk = byteArrayOf()))
            )
            logger.debug { "$direction$RESPONSE_PREFIX$sanitized" }
        } else {
            logger.debug { "$direction$RESPONSE_PREFIX$response" }
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
        val headers = currentHeaders()
        return buildString {
            append("channel=").append(headers.getHeaderOrNull(RequestHeader.CHANNEL) ?: "")
            append(", platform=").append(headers.getHeaderOrNull(RequestHeader.PLATFORM) ?: "")
            append(", x-request-id=").append(headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID) ?: "")
        }
    }

    private companion object {
        private const val REQUEST_PREFIX = "Request] -> "
        private const val RESPONSE_PREFIX = "Response] -> "
        private const val DIRECTION_IN = "[IN:"
        private const val DIRECTION_OUT = "[OUT:"
    }
}
