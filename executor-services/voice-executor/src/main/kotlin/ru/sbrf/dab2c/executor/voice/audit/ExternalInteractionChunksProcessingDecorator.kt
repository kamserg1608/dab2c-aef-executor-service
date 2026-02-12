package ru.sbrf.dab2c.executor.voice.audit

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
                lastRqMessage = serializeRequestChunk(chunk)
            }
            .catch { e ->
                val metadata = GrpcMetadataContext.fromGrpcThread()
                val cookie = metadata.ufsCookie

                runCatching {
                    sendFailure(
                        errorCode = AuditMessageSchema.ERROR_CODE_VOICE_REQUEST_STREAM,
                        errorTitle = e.message,
                        cookie = cookie
                    )
                }

                throw e
            }

        return delegate.processRequestChunks(auditedInput)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val auditedOutput = responsesChunks
            .onEach { chunk ->
                lastRsMessage = toJson(chunk)
            }
            .onCompletion { cause ->
                handleResponseCompletion(cause)
            }

        return delegate.processResponseChunks(auditedOutput)
    }

    private suspend fun handleResponseCompletion(cause: Throwable?) {
        val metadata = currentRequestMetadata()
        val cookie = metadata.ufsCookie

        if (cause == null) {
            runCatching {
                auditor.success(
                    request = ExternalInteractionRequest(
                        answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                        rqMessage = lastRqMessage,
                        rsMessage = lastRsMessage
                    ),
                    cookie = cookie
                )
            }
            return
        }

        runCatching {
            sendFailure(
                errorCode = AuditMessageSchema.ERROR_CODE_VOICE_RESPONSE_STREAM,
                errorTitle = cause.message,
                cookie = cookie
            )
        }
    }

    private suspend fun sendFailure(
        errorCode: String,
        errorTitle: String?,
        cookie: String
    ) {
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

    private fun serializeRequestChunk(chunk: VoiceRequest): String =
        if (chunk is VoiceRequest.Audio) {
            serializeAudioChunk(chunk)
        } else {
            toJson(chunk)
        }

    private fun serializeAudioChunk(chunk: VoiceRequest.Audio): String =
        toJson(
            mapOf(
                AuditMessageSchema.KEY_TYPE to AuditMessageSchema.TYPE_AUDIO,
                AuditMessageSchema.KEY_SPEECH_START to chunk.content.speechStart,
                AuditMessageSchema.KEY_SPEECH_END to chunk.content.speechEnd,
                AuditMessageSchema.KEY_AUDIO_PRESENT to (chunk.content.audioChunk != null),
                AuditMessageSchema.KEY_AUDIO_SIZE to (chunk.content.audioChunk?.size ?: 0)
            )
        )

    private fun toJson(value: Any?): String =
        objectMapper.writeValueAsString(value)
}
