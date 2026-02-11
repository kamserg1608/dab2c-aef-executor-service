package ru.sbrf.dab2c.executor.voice.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.model.ufsCookie
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

private val logger = KotlinLogging.logger {}

/**
 * Audit decorator for outer voice interaction.
 *
 * Emits:
 *  - DAB2C_EXTERNAL_INTERACTION
 *  - DAB2C_EXTERNAL_INTERACTION_FAILED
 *
 * Wraps request/response streams of [ChunkProcessingService].
 */
class ExternalInteractionChunksProcessingDecorator(
    private val delegate: ChunkProcessingService,
    private val auditor: ExternalInteractionAuditor
) : ChunkProcessingService {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @Volatile
    private var lastRqMessage: String? = null

    @Volatile
    private var lastRsMessage: String? = null

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        val auditedInput = requestsChunks
            .onEach { chunk ->
                lastRqMessage = safeSerializeRequestChunk(chunk)
            }
            .catch { e ->
                logger.warn(e) { "Voice request stream failed: ${e.message}" }
                val requestMetadata = GrpcMetadataContext.fromGrpcThread()
                sendFailure(
                    errorCode = AuditMessageSchema.ERROR_CODE_VOICE_REQUEST_STREAM,
                    errorTitle = e.message,
                    cookie = requestMetadata.ufsCookie
                )
                throw e
            }

        return delegate.processRequestChunks(auditedInput)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val auditedOutput = responsesChunks
            .onEach(::handleResponseChunk)
            .onCompletion { cause -> handleResponseCompletion(cause) }
            .catch { e -> throw e }

        return delegate.processResponseChunks(auditedOutput)
    }

    private fun handleResponseChunk(chunk: VoiceResponse) {
        lastRsMessage = safeSerializeAny(chunk)
    }

    private suspend fun handleResponseCompletion(cause: Throwable?) {
        val metadata = currentRequestMetadata()
        val cookie = metadata.ufsCookie

        if (cause == null) {
            auditor.success(
                request = ExternalInteractionRequest(
                    answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                    rqMessage = lastRqMessage,
                    rsMessage = lastRsMessage
                ),
                cookie = cookie
            )
            return
        }

        logger.warn(cause) { "Voice response stream completed with error: ${cause.message}" }
        sendFailure(
            errorCode = AuditMessageSchema.ERROR_CODE_VOICE_RESPONSE_STREAM,
            errorTitle = cause.message,
            cookie = cookie
        )
    }

    private suspend fun sendFailure(errorCode: String, errorTitle: String?, cookie: String) {
        auditor.failed(
            request = ExternalInteractionRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                rqMessage = lastRqMessage,
                rsMessage = lastRsMessage,
                errorCode = errorCode,
                errorTitle = errorTitle ?: AuditMessageSchema.ERROR_TITLE_VOICE_STREAM
            ),
            cookie = cookie
        )
    }

    private fun safeSerializeRequestChunk(chunk: VoiceRequest): String =
        runCatching {
            if (chunk is VoiceRequest.Audio) {
                serializeAudioChunk(chunk)
            } else {
                objectMapper.writeValueAsString(chunk)
            }
        }.getOrElse {
            "${chunk::class.simpleName}(serializationError=${it.message})"
        }

    private fun serializeAudioChunk(chunk: VoiceRequest.Audio): String =
        objectMapper.writeValueAsString(
            mapOf(
                AuditMessageSchema.KEY_TYPE to AuditMessageSchema.TYPE_AUDIO,
                AuditMessageSchema.KEY_SPEECH_START to chunk.content.speechStart,
                AuditMessageSchema.KEY_SPEECH_END to chunk.content.speechEnd,
                AuditMessageSchema.KEY_AUDIO_PRESENT to (chunk.content.audioChunk != null),
                AuditMessageSchema.KEY_AUDIO_SIZE to (chunk.content.audioChunk?.size ?: 0)
            )
        )

    private fun safeSerializeAny(value: Any?): String =
        runCatching { objectMapper.writeValueAsString(value) }
            .getOrElse { "${value?.javaClass?.simpleName}(serializationError=${it.message})" }
}
