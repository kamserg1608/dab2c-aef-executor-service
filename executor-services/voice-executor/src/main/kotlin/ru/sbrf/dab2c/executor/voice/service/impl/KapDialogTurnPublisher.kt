package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogEnvelopeMapper
import ru.sbrf.dab2c.executor.clients.kap.producer.mapper.DialogTurnData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnEvent
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.clients.kap.producer.model.ErrorPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallDetails
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionResultPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.WarningPayload
import ru.sbrf.dab2c.executor.library.context.RequestHeader
import ru.sbrf.dab2c.executor.library.context.currentHeaders
import ru.sbrf.dab2c.executor.library.context.currentSessionInfo
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.model.currentFeatureToggles
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionResultSent
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.TurnEvent
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSessionObserver
import ru.sbrf.dab2c.executor.voice.session.observer.WarningEmitted
import java.util.UUID

/** Publishes dialog turn events to KAP. Stateful — one instance per voice session. */
class KapDialogTurnPublisher(
    private val kapProducerClient: KapProducerClient,
    private val session: VoiceSession
) : VoiceSessionObserver {

    private var previousMessageId: String? = null

    override suspend fun onTurnCompleted(event: TurnCompleted) {
        val sendExtras = currentFeatureToggles().kapSendExtra
        publishDialogTurn(
            inputText = event.userReplica?.text.orEmpty(),
            outputText = event.assistantReplica?.text.orEmpty(),
            assistantResponseTime = event.assistantReplica?.let { it.endedAtMs - it.startedAtMs } ?: 0L,
            extra = if (sendExtras) toKapExtra(event) else null,
            totalTokens = if (sendExtras) event.totalTokens else null,
        )
    }

    /** Builds and publishes a single dialog envelope; no-op outside the `Serving` state. */
    @Suppress("LongParameterList")
    suspend fun publishDialogTurn(
        inputText: String,
        outputText: String,
        assistantResponseTime: Long,
        extra: DialogTurnExtra? = null,
        totalTokens: Int? = null,
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

    private fun toKapExtra(event: TurnCompleted): DialogTurnExtra = DialogTurnExtra(
        events = event.turnEvents.map(::toKapEvent),
        userMessageStartTS = event.userReplica?.startedAtMs,
        userMessageEndTS = event.userReplica?.endedAtMs,
        assistantMessageStartTS = event.assistantReplica?.startedAtMs,
        assistantMessageEndTS = event.assistantReplica?.endedAtMs,
    )

    private fun toKapEvent(turnEvent: TurnEvent): DialogTurnEvent = when (turnEvent) {
        is FunctionCallReceived -> DialogTurnEvent(
            eventName = EVENT_FUNCTION_CALL,
            timestamp = turnEvent.atMs,
            payload = FunctionCallPayload(
                functionCall = FunctionCallDetails(name = turnEvent.name, arguments = turnEvent.arguments),
                timestamp = turnEvent.atMs,
            )
        )
        is FunctionResultSent -> DialogTurnEvent(
            eventName = EVENT_FUNCTION_RESULT,
            timestamp = turnEvent.atMs,
            payload = FunctionResultPayload(content = turnEvent.content, functionName = turnEvent.name)
        )
        is WarningEmitted -> DialogTurnEvent(
            eventName = EVENT_WARNING,
            timestamp = turnEvent.atMs,
            payload = WarningPayload(message = turnEvent.message)
        )
        is ErrorEmitted -> DialogTurnEvent(
            eventName = EVENT_ERROR,
            timestamp = turnEvent.atMs,
            payload = ErrorPayload(status = turnEvent.status, message = turnEvent.message)
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private const val MILLIS_TO_SECONDS = 1000L

        private const val EVENT_FUNCTION_CALL = "function_call"
        private const val EVENT_FUNCTION_RESULT = "function_result"
        private const val EVENT_WARNING = "warning"
        private const val EVENT_ERROR = "error"
    }
}
