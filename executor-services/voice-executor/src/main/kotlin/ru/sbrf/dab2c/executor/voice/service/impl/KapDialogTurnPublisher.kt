package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogTurnData
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import ru.sbrf.dab2c.executor.voice.util.extensions.currentRequestMetadata
import java.util.UUID

/**
 * Publishes dialog turns to KAP (Kafka Analytics Platform).
 */
class KapDialogTurnPublisher(
    private val kapProducerClient: KapProducerClient,
    private val processingState: StateFlow<ProcessingState>
) : DialogTurnPublisher {

    private var previousMessageId: String? = null

    override suspend fun publishDialogTurn(inputText: String, outputText: String) {
        val state = processingState.value
        if (state !is ProcessingState.Serving) {
            logger.warn { "Cannot publish dialog - not in Serving state, current state: ${state::class.simpleName}" }
            return
        }

        logger.info { "Publishing dialog turn: input='$inputText', output='$outputText'" }

        val metadata = currentRequestMetadata()
        val assistantMessageId = UUID.randomUUID().toString()

        val dialogTurnData = DialogTurnData(
            envelopeId = UUID.randomUUID().toString(),
            userMessageId = UUID.randomUUID().toString(),
            assistantMessageId = assistantMessageId,
            inputText = inputText,
            outputText = outputText,
            chatId = state.conversationId,
            timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
            previousMessageId = previousMessageId,
            daSessionInfo = metadata.daSessionInfo
        )

        val dialogEnvelope = DialogEnvelopeMapper.toDialogEnvelope(dialogTurnData)

        previousMessageId = assistantMessageId

        kapProducerClient.publishDialog(dialogEnvelope)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
