package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogTurnData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.DialogTurnPublisher
import java.util.UUID

/** Publishes dialog turn events to KAP. */
class KapDialogTurnPublisher(
    private val kapProducerClient: KapProducerClient,
    private val session: VoiceSession
) : DialogTurnPublisher {

    private var previousMessageId: String? = null

    @Suppress("LongParameterList")
    override suspend fun publishDialogTurn(
        inputText: String,
        outputText: String,
        assistantResponseTime: Long,
        extra: DialogTurnExtra?,
        totalTokens: Int?
    ) {
        val state = session.state.value
        if (state !is ProcessingState.Serving) {
            logger.warn { "Cannot publish dialog - not in Serving state, current state: ${state::class.simpleName}" }
            return
        }
        logger.info {
            "Publishing dialog turn: input='$inputText', output='$outputText', responseTime=${assistantResponseTime}ms"
        }
        val assistantMessageId = session.turnIds.assistantMessageId
        val dialogTurnData = buildTurnData(
            state, inputText, outputText, assistantResponseTime, extra, totalTokens, assistantMessageId
        )
        publishTurnData(dialogTurnData, assistantMessageId)
    }

    @Suppress("LongParameterList")
    private suspend fun buildTurnData(
        state: ProcessingState.Serving,
        inputText: String,
        outputText: String,
        assistantResponseTime: Long,
        extra: DialogTurnExtra?,
        totalTokens: Int?,
        assistantMessageId: String
    ): DialogTurnData = DialogTurnData(
        envelopeId = UUID.randomUUID().toString(),
        userMessageId = session.turnIds.userMessageId,
        assistantMessageId = assistantMessageId,
        inputText = inputText,
        outputText = outputText,
        chatId = state.conversationId,
        timestamp = System.currentTimeMillis() / MILLIS_TO_SECONDS,
        previousMessageId = previousMessageId,
        daSessionInfo = currentSessionInfo(),
        agentCi = state.agentConfiguration.functionalSubsystemCi,
        assistantResponseTime = assistantResponseTime,
        requestId = currentHeaders().getHeaderOrNull(RequestHeader.X_REQUEST_ID),
        extra = extra,
        totalTokens = totalTokens
    )

    private suspend fun publishTurnData(dialogTurnData: DialogTurnData, assistantMessageId: String) {
        val dialogEnvelope = DialogEnvelopeMapper.toDialogEnvelope(dialogTurnData)
        logger.debug { "Dialog envelope content: $dialogEnvelope" }
        previousMessageId = assistantMessageId
        kapProducerClient.publishDialog(dialogEnvelope)
        session.turnIds.rotate()
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L
    }
}
