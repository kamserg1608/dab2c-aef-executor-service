package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher

/**
 * ChunkProcessingService decorator that accumulates dialog transcriptions and publishes them.
 *
 * Accumulates InputTranscription chunks until OutputTranscription arrives,
 * then accumulates OutputTranscription chunks until next InputTranscription.
 * When a complete input-output pair is collected, publishes the dialog turn.
 */
class DialogAccumulatorDelegate(
    private val delegate: ChunkProcessingService,
    private val dialogTurnPublisher: DialogTurnPublisher
) : ChunkProcessingService {

    private val inputChunks = mutableListOf<String>()
    private val outputChunks = mutableListOf<String>()
    private var phase = Phase.AWAITING_INPUT
    private var outputStartTimestamp: Long = 0L
    private var outputEndTimestamp: Long = 0L

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> =
        delegate.processRequestChunks(requestsChunks)

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val accumulatedChunks = responsesChunks
            .onEach { response ->
                when (response) {
                    is VoiceResponse.InputTranscription -> handleInputTranscription(response)
                    is VoiceResponse.OutputTranscription -> handleOutputTranscription(response)
                    else -> { /* pass through other response types */ }
                }
            }
            .onCompletion { flushPendingDialogTurn() }
        return delegate.processResponseChunks(accumulatedChunks)
    }

    private suspend fun flushPendingDialogTurn() {
        if (phase == Phase.ACCUMULATING_OUTPUT && inputChunks.isNotEmpty() && outputChunks.isNotEmpty()) {
            logger.debug { "Flushing final dialog turn on session completion" }
            publishDialogTurn()
            reset()
        }
    }

    private suspend fun handleInputTranscription(response: VoiceResponse.InputTranscription) {
        val text = response.transcription.text

        if (phase == Phase.ACCUMULATING_OUTPUT) {
            publishDialogTurn()
            reset()
        }

        phase = Phase.ACCUMULATING_INPUT
        inputChunks.add(text)
        logger.debug { "Accumulated input chunk: '$text'" }
    }

    private fun handleOutputTranscription(response: VoiceResponse.OutputTranscription) {
        val text = response.transcription.text

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

    private enum class Phase {
        AWAITING_INPUT,
        ACCUMULATING_INPUT,
        ACCUMULATING_OUTPUT
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
