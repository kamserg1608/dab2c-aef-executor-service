package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/**
 * ChunkProcessingService decorator that accumulates dialog transcriptions.
 *
 * Accumulates InputTranscription chunks until OutputTranscription arrives,
 * then accumulates OutputTranscription chunks until next InputTranscription.
 * When a complete input-output pair is collected, logs the dialog turn.
 */
class DialogAccumulatorDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger {}

    private val inputChunks = mutableListOf<String>()
    private val outputChunks = mutableListOf<String>()
    private var phase = Phase.AWAITING_INPUT

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> =
        delegate.processRequestChunks(requestsChunks)

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        val accumulatedChunks = responsesChunks.onEach { response ->
            when (response) {
                is VoiceResponse.InputTranscription -> handleInputTranscription(response)
                is VoiceResponse.OutputTranscription -> handleOutputTranscription(response)
                else -> { /* pass through other response types */ }
            }
        }
        return delegate.processResponseChunks(accumulatedChunks)
    }

    private fun handleInputTranscription(response: VoiceResponse.InputTranscription) {
        val text = response.transcription.text

        if (phase == Phase.ACCUMULATING_OUTPUT) {
            logDialogTurn()
            reset()
        }

        phase = Phase.ACCUMULATING_INPUT
        inputChunks.add(text)
        logger.debug { "Accumulated input chunk: '$text'" }
    }

    private fun handleOutputTranscription(response: VoiceResponse.OutputTranscription) {
        val text = response.transcription.text

        phase = Phase.ACCUMULATING_OUTPUT
        outputChunks.add(text)
        logger.debug { "Accumulated output chunk: '$text'" }
    }

    private fun logDialogTurn() {
        val inputPhrase = inputChunks.joinToString("")
        val outputPhrase = outputChunks.joinToString("")

        logger.info {
            "Dialog turn completed: input='$inputPhrase', output='$outputPhrase'"
        }
    }

    private fun reset() {
        inputChunks.clear()
        outputChunks.clear()
    }

    private enum class Phase {
        AWAITING_INPUT,
        ACCUMULATING_INPUT,
        ACCUMULATING_OUTPUT
    }
}
