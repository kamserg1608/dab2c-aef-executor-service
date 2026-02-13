package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionAuditor
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionRequest
import ru.sbrf.dab2c.executor.voice.model.ufsCookie
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata

/**
 * ChunkProcessingService decorator that accumulates dialog transcriptions and publishes them.
 *
 * Accumulates InputTranscription chunks until OutputTranscription arrives,
 * then accumulates OutputTranscription chunks until next InputTranscription.
 * When a complete input-output pair is collected, publishes the dialog turn.
 */
class DialogAccumulatorDelegate(
    private val delegate: ChunkProcessingService,
    private val dialogTurnPublisher: DialogTurnPublisher,
    private val auditor: ExternalInteractionAuditor
) : ChunkProcessingService {

    private val objectMapper = jacksonObjectMapper()

    private val inputChunks = mutableListOf<String>()
    private val outputChunks = mutableListOf<String>()
    private val dialog = StringBuilder()

    private var settingsJson: String? = null

    private var phase = Phase.AWAITING_INPUT
    private var outputStartTimestamp: Long = 0L
    private var outputEndTimestamp: Long = 0L

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> =
        delegate.processRequestChunks(requestsChunks).onEach { request ->
            if (request is VoiceRequest.Settings) {
                handleVoiceSettings(request)
            }
        }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val accumulatedChunks = responsesChunks
            .onEach { response ->
                when (response) {
                    is VoiceResponse.InputTranscription -> handleInputTranscription(response)
                    is VoiceResponse.OutputTranscription -> handleOutputTranscription(response)
                    is VoiceResponse.Warning -> handleWarning(response)
                    is VoiceResponse.Error -> handleError(response)
                    else -> Unit
                }
            }
            .onCompletion { cause ->
                sendAuditOnCompletion(cause)
                flushPendingDialogTurn()
            }

        return delegate.processResponseChunks(accumulatedChunks)
    }

    private fun handleVoiceSettings(request: VoiceRequest.Settings) {
        settingsJson = objectMapper.writeValueAsString(request.settings)
    }

    private suspend fun sendAuditOnCompletion(cause: Throwable?) {
        val cookie = currentRequestMetadata().ufsCookie
        val rqMessage = dialog.toString().ifBlank { null }
        val rsMessage = settingsJson

        if (cause == null || isClientCancellation(cause)) {
            sendSuccessAudit(cookie, rqMessage, rsMessage)
            return
        }

        sendFailedAudit(cookie, rqMessage, rsMessage, cause)
    }

    private fun isClientCancellation(cause: Throwable): Boolean =
        cause is CancellationException && cause.message?.contains("client", ignoreCase = true) == true

    private suspend fun sendSuccessAudit(cookie: String, rqMessage: String?, rsMessage: String?) {
        auditor.success(
            request = ExternalInteractionRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                rqMessage = rqMessage,
                rsMessage = rsMessage
            ),
            cookie = cookie
        )
    }

    private suspend fun sendFailedAudit(
        cookie: String,
        rqMessage: String?,
        rsMessage: String?,
        cause: Throwable
    ) {
        auditor.failed(
            request = ExternalInteractionRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_FAIL,
                rqMessage = rqMessage,
                rsMessage = rsMessage,
                errorCode = AuditMessageSchema.ERROR_CODE_VOICE_RESPONSE_STREAM,
                errorTitle = cause.message ?: AuditMessageSchema.ERROR_TITLE_VOICE_STREAM
            ),
            cookie = cookie
        )
    }

    private suspend fun flushPendingDialogTurn() {
        if (
            phase == Phase.ACCUMULATING_OUTPUT &&
            inputChunks.isNotEmpty() &&
            outputChunks.isNotEmpty()
        ) {
            logger.debug { "Flushing final dialog turn on session completion" }
            publishDialogTurn()
        }

        reset()
    }

    private suspend fun handleInputTranscription(
        response: VoiceResponse.InputTranscription
    ) {
        val text = response.transcription.text

        if (phase == Phase.ACCUMULATING_OUTPUT) {
            publishDialogTurn()
            appendDialogTurnToHistory()
            reset()
        }

        phase = Phase.ACCUMULATING_INPUT
        inputChunks.add(text)

        logger.debug { "Accumulated input chunk: '$text'" }
    }

    private fun appendDialogTurnToHistory() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("")

        appendDialogLine(ROLE_USER, inputPhrase)
        appendDialogLine(ROLE_ASSISTANT, outputPhrase)
    }

    private fun handleOutputTranscription(
        response: VoiceResponse.OutputTranscription
    ) {
        val text = response.transcription.text
        appendAssistantChunk(text)
    }

    private fun handleWarning(response: VoiceResponse.Warning) {
        val text = "$WARNING_PREFIX${response.warning.message}"
        appendAssistantChunk(text)
        appendDialogLine(ROLE_ASSISTANT, text)
    }

    private fun handleError(response: VoiceResponse.Error) {
        val text = "$ERROR_PREFIX${response.error.status}] ${response.error.message}"
        appendAssistantChunk(text)
        appendDialogLine(ROLE_ASSISTANT, text)
    }

    private fun appendAssistantChunk(text: String) {
        if (outputChunks.isEmpty()) {
            outputStartTimestamp = System.currentTimeMillis()
        }

        phase = Phase.ACCUMULATING_OUTPUT
        outputChunks.add(text)
        outputEndTimestamp = System.currentTimeMillis()

        logger.debug { "Accumulated output chunk: '$text'" }
    }

    private suspend fun publishDialogTurn() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("")
        val assistantResponseTime = outputEndTimestamp - outputStartTimestamp

        logger.info {
            "Dialog turn completed: input='$inputPhrase', output='$outputPhrase', " +
                "assistantResponseTime=${assistantResponseTime}ms"
        }

        dialogTurnPublisher.publishDialogTurn(inputPhrase, outputPhrase, assistantResponseTime)
    }

    private fun reset() {
        inputChunks.clear()
        outputChunks.clear()
        outputStartTimestamp = 0L
        outputEndTimestamp = 0L
        phase = Phase.AWAITING_INPUT
    }

    private fun appendDialogLine(prefix: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        if (dialog.isNotEmpty()) {
            dialog.append('\n')
        }

        dialog.append(prefix)
            .append(": ")
            .append(trimmed)
    }

    private enum class Phase {
        AWAITING_INPUT,
        ACCUMULATING_INPUT,
        ACCUMULATING_OUTPUT
    }

    private companion object {
        private val logger = KotlinLogging.logger {}

        private const val ROLE_USER = "USER"
        private const val ROLE_ASSISTANT = "ASSISTANT"
        private const val WARNING_PREFIX = "[WARNING] "
        private const val ERROR_PREFIX = "[ERROR] "
    }
}
