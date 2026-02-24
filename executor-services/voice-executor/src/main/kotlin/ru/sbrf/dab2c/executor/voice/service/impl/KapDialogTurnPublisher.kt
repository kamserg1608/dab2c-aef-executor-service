package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.StateFlow
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogTurnData
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import java.util.UUID

/** Publishes dialog turn events to KAP. */
class KapDialogTurnPublisher(
    private val kapProducerClient: KapProducerClient,
    private val processingState: StateFlow<ProcessingState>
) : DialogTurnPublisher {

    private var previousMessageId: String? = null

    override suspend fun publishDialogTurn(inputText: String, outputText: String, assistantResponseTime: Long) {
        val state = processingState.value
        if (state !is ProcessingState.Serving) {
            logger.warn { "Cannot publish dialog - not in Serving state, current state: ${state::class.simpleName}" }
            return
        }

        logger.info {
            "Publishing dialog turn: input='$inputText', output='$outputText', responseTime=${assistantResponseTime}ms"
        }

        val headers = currentHeaders()
        val daSessionInfo = currentSessionInfo()
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
            daSessionInfo = daSessionInfo,
            agentCi = state.agentConfiguration.functionalSubsystemCi,
            assistantResponseTime = assistantResponseTime,
            requestId = headers.getHeaderOrNull(RequestHeader.X_REQUEST_ID)
        )

        publishTurnData(dialogTurnData, assistantMessageId)
    }

    private suspend fun publishTurnData(dialogTurnData: DialogTurnData, assistantMessageId: String) {
        val dialogEnvelope = DialogEnvelopeMapper.toDialogEnvelope(dialogTurnData)
        previousMessageId = assistantMessageId
        kapProducerClient.publishDialog(dialogEnvelope)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
