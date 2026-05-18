package ru.sbrf.dab2c.executor.voice.service.impl

import com.google.protobuf.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.ContentFromModel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.logging.IntegrationLogger
import ru.sbrf.dab2c.executor.voice.exception.TolerantExceptionRegistry
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/** Decorator that adds request/response logging to chunk processing. */
class LoggingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }
    private val sessionStartTime = System.currentTimeMillis()

    override fun processRequestChunks(requestsChunks: Flow<GigaVoiceRequest>): Flow<GigaVoiceRequest> {
        val observedChunks = requestsChunks
            .onStart { logSessionStart() }
            .onEach { request -> logRequest(DIRECTION_IN, request) }
        return delegate.processRequestChunks(observedChunks)
            .onEach { request -> logRequest(DIRECTION_OUT, request) }
    }

    override fun processResponseChunks(responsesChunks: Flow<GigaVoiceResponse>): Flow<GigaVoiceResponse> {
        val observedChunks = responsesChunks
            .onEach { response -> logResponse(DIRECTION_IN, response) }
        return delegate.processResponseChunks(observedChunks)
            .onEach { response -> logResponse(DIRECTION_OUT, response) }
            .onCompletion { error -> logSessionEnd(error) }
    }

    private fun logRequest(direction: String, request: GigaVoiceRequest) {
        val sanitized = if (
            request.requestCase == GigaVoiceRequest.RequestCase.INPUT &&
            request.input.hasAudioContent()
        ) {
            request.toBuilder().apply { inputBuilder.audioContentBuilder.clearAudioChunk() }.build()
        } else {
            request
        }
        logger.debug { "$direction$REQUEST_PREFIX${toJson(sanitized)}" }
    }

    private fun logResponse(direction: String, response: GigaVoiceResponse) {
        if (response.responseCase == GigaVoiceResponse.ResponseCase.SERVICE_INFO) {
            logSessionInfo(response)
            return
        }
        val sanitized = if (
            response.responseCase == GigaVoiceResponse.ResponseCase.OUTPUT &&
            response.output.responseCase == ContentFromModel.ResponseCase.AUDIO
        ) {
            response.toBuilder().apply { outputBuilder.audioBuilder.clearAudioChunk() }.build()
        } else {
            response
        }
        logger.debug { "$direction$RESPONSE_PREFIX${toJson(sanitized)}" }
    }

    private fun toJson(message: Message): String = ObjectMappers.MAPPER.writeValueAsString(message)

    private suspend fun logSessionStart() {
        val metadataString = buildMetadataString()
        IntegrationLogger.logGrpcEvent(
            message = "gRPC session started",
            rqMessage = metadataString
        )
    }

    private fun logSessionEnd(error: Throwable?) {
        val executionTime = System.currentTimeMillis() - sessionStartTime
        if (error != null && TolerantExceptionRegistry.isTolerant(error)) {
            IntegrationLogger.logGrpcEvent(
                message = "gRPC session completed (${error::class.simpleName})",
                executionTime = executionTime
            )
        } else {
            IntegrationLogger.logGrpcEvent(
                message = if (error == null) "gRPC session completed" else "gRPC session failed",
                executionTime = executionTime,
                error = error
            )
        }
    }

    private fun logSessionInfo(response: GigaVoiceResponse) {
        IntegrationLogger.logGrpcEvent(
            message = "gRPC session info ${toJson(response)}",
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
