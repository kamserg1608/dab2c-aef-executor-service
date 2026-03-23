package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnEvent
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.clients.kap.producer.model.ErrorPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallDetails
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionResultPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.WarningPayload
import ru.sbrf.dab2c.executor.domain.voice.ContentFromModel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.library.audit.model.AuditMessageSchema
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.time.TimeProvider
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionAuditor
import ru.sbrf.dab2c.executor.voice.audit.ExternalInteractionRequest
import ru.sbrf.dab2c.executor.voice.model.currentFeatureToggles
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import java.util.Collections
import kotlin.reflect.KClass

/** Per-turn timestamp boundaries for dialog metrics. */
private data class TurnTimestamps(
    var outputStart: Long = 0L,
    var outputEnd: Long = 0L,
    var userMessageStart: Long? = null,
    var userMessageEnd: Long? = null,
    var assistantMessageStart: Long? = null,
    var assistantMessageEnd: Long? = null
)

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
    private val auditor: ExternalInteractionAuditor,
    private val timeProvider: TimeProvider
) : ChunkProcessingService {

    private val inputChunks = mutableListOf<String>()
    private val outputChunks = mutableListOf<String>()
    private val dialog = StringBuilder()

    private var settingsJson: String? = null
    private var phase = Phase.AWAITING_INPUT
    private var timestamps = TurnTimestamps()

    private var totalTokens: Int = 0
    private val turnEvents: MutableList<DialogTurnEvent> =
        Collections.synchronizedList(mutableListOf())

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> =
        delegate.processRequestChunks(requestsChunks).onEach { request ->
            when (request) {
                is VoiceRequest.Settings -> handleVoiceSettings(request)
                is VoiceRequest.Audio -> handleAudioRequest()
                is VoiceRequest.FunctionResult -> handleFunctionResult(request)
                else -> Unit
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
                    is VoiceResponse.FunctionCalling -> handleFunctionCalling(response)
                    is VoiceResponse.Output -> handleOutput(response)
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
        settingsJson = ObjectMappers.MAPPER.writeValueAsString(request.settings)
    }

    private fun handleAudioRequest() {
        if (timestamps.userMessageStart == null) {
            timestamps.userMessageStart = timeProvider.currentTimeMillis()
        }
    }

    private fun handleFunctionResult(request: VoiceRequest.FunctionResult) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_FUNCTION_RESULT,
                timestamp = timeProvider.currentTimeMillis(),
                payload = FunctionResultPayload(
                    content = request.result.content,
                    functionName = request.result.functionName ?: ""
                )
            )
        )
    }

    private fun handleFunctionCalling(response: VoiceResponse.FunctionCalling) {
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_FUNCTION_CALL,
                timestamp = timeProvider.currentTimeMillis(),
                payload = FunctionCallPayload(
                    functionCall = FunctionCallDetails(
                        name = response.data.functionCall.name,
                        arguments = response.data.functionCall.arguments
                    ),
                    timestamp = response.data.timestamp
                )
            )
        )
    }

    private fun handleOutput(response: VoiceResponse.Output) {
        if (timestamps.assistantMessageStart == null) {
            timestamps.assistantMessageStart = timeProvider.currentTimeMillis()
        }
        val content = response.content
        if (content is ContentFromModel.Audio && content.audio.isFinal) {
            timestamps.assistantMessageEnd = timeProvider.currentTimeMillis()
        }
        if (content is ContentFromModel.AdditionalData) {
            totalTokens += content.data.usage?.totalTokens ?: 0
        }
    }

    private suspend fun handleInputTranscription(
        response: VoiceResponse.InputTranscription
    ) {
        val text = response.transcription.text

        timestamps.userMessageEnd = timeProvider.currentTimeMillis()

        if (phase == Phase.ACCUMULATING_OUTPUT) {
            publishDialogTurn()
            appendDialogTurnToHistory()
            reset()
        }

        phase = Phase.ACCUMULATING_INPUT
        inputChunks.add(text)

        logger.debug { "Accumulated input chunk: '$text'" }
    }

    private fun handleOutputTranscription(
        response: VoiceResponse.OutputTranscription
    ) {
        if (outputChunks.isEmpty()) {
            timestamps.outputStart = timeProvider.currentTimeMillis()
        }

        phase = Phase.ACCUMULATING_OUTPUT
        outputChunks.add(response.transcription.text)
        timestamps.outputEnd = timeProvider.currentTimeMillis()

        logger.debug { "Accumulated output chunk: '${response.transcription.text}'" }
    }

    private fun handleWarning(response: VoiceResponse.Warning) {
        appendDialogLine(WARNING_PREFIX, response.warning.message)
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_WARNING,
                timestamp = timeProvider.currentTimeMillis(),
                payload = WarningPayload(message = response.warning.message)
            )
        )
    }

    private fun handleError(response: VoiceResponse.Error) {
        appendDialogLine(ERROR_PREFIX, "${response.error.status} ${response.error.message}")
        turnEvents.add(
            DialogTurnEvent(
                eventName = EVENT_ERROR,
                timestamp = timeProvider.currentTimeMillis(),
                payload = ErrorPayload(
                    status = response.error.status,
                    message = response.error.message
                )
            )
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
    }

    private fun appendDialogTurnToHistory() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("\n")

        appendDialogLine(ROLE_USER, inputPhrase)
        appendDialogLine(ROLE_ASSISTANT, outputPhrase)
    }

    private fun handleException(cause: Throwable) {
        appendDialogLine(EXCEPTION_PREFIX, "${cause::class.simpleName} ${cause.message}")
    }

    private fun appendDialogLine(prefix: String, text: String) {
        if (text.isNotBlank()) dialog.append("$prefix${text.trim()}\n")
    }

    private suspend fun publishDialogTurn() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("")
        val assistantResponseTime = timestamps.outputEnd - timestamps.outputStart
        val kapSendExtra = currentFeatureToggles().kapSendExtra
        val extra = if (kapSendExtra) buildExtra() else null
        val totalTokens = if (kapSendExtra) totalTokens else null

        logger.info {
            "Dialog turn completed: input='$inputPhrase', output='$outputPhrase', " +
                "assistantResponseTime=${assistantResponseTime}ms"
        }

        dialogTurnPublisher.publishDialogTurn(
            inputPhrase, outputPhrase, assistantResponseTime, extra, totalTokens
        )
    }

    private fun buildExtra(): DialogTurnExtra =
        DialogTurnExtra(
            events = ArrayList(turnEvents),
            userMessageStartTS = timestamps.userMessageStart ?: timeProvider.currentTimeMillis(),
            userMessageEndTS = timestamps.userMessageEnd ?: timeProvider.currentTimeMillis(),
            assistantMessageStartTS = timestamps.assistantMessageStart ?: timeProvider.currentTimeMillis(),
            assistantMessageEndTS = timestamps.assistantMessageEnd ?: timeProvider.currentTimeMillis()
        )

    private suspend fun sendSuccessAudit(rqMessage: String?, rsMessage: String?) {
        auditor.success(
            request = ExternalInteractionRequest(
                answerCode = AuditMessageSchema.ANSWER_CODE_OK,
                rqMessage = rqMessage,
                rsMessage = rsMessage
            )
        )
    }

    private suspend fun sendFailedAudit(
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
            )
        )
    }

    private suspend fun sendAuditOnCompletion(cause: Throwable?) {
        val rsMessage = settingsJson

        appendDialogTurnToHistory()

        if (cause != null) {
            handleException(cause)
        }

        val rqMessage = dialog.toString().trim().ifBlank { null }

        if (cause == null || isNonFailureException(cause)) {
            sendSuccessAudit(rqMessage, rsMessage)
            return
        }

        sendFailedAudit(rqMessage, rsMessage, cause)
    }

    private fun isNonFailureException(cause: Throwable): Boolean =
        NON_FAILURE_EXCEPTIONS.any { it.isInstance(cause) }

    private fun reset() {
        inputChunks.clear()
        outputChunks.clear()
        phase = Phase.AWAITING_INPUT
        timestamps = TurnTimestamps()
        totalTokens = 0
        turnEvents.clear()
    }

    private enum class Phase {
        AWAITING_INPUT,
        ACCUMULATING_INPUT,
        ACCUMULATING_OUTPUT
    }

    private companion object {
        private val logger = KotlinLogging.logger {}

        private const val ROLE_USER = "USER: "
        private const val ROLE_ASSISTANT = "ASSISTANT: "
        private const val WARNING_PREFIX = "[WARNING] "
        private const val ERROR_PREFIX = "[ERROR] "
        private const val EXCEPTION_PREFIX = "[EXCEPTION] "

        private const val EVENT_FUNCTION_CALL = "function_call"
        private const val EVENT_FUNCTION_RESULT = "function_result"
        private const val EVENT_WARNING = "warning"
        private const val EVENT_ERROR = "error"

        private val NON_FAILURE_EXCEPTIONS: List<KClass<out Throwable>> = listOf(
            CancellationException::class
        )
    }
}
