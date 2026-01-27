@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentId
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AssistantMessage
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.UserMessage
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo

/**
 * Input data for mapping a dialog turn to DialogEnvelope.
 */
data class DialogTurnData(
    val envelopeId: String,
    val userMessageId: String,
    val assistantMessageId: String,
    val inputText: String,
    val outputText: String,
    val chatId: String,
    val timestamp: Long,
    val previousMessageId: String?,
    val daSessionInfo: DaSessionInfo,
    val agentCi: String,
    val assistantResponseTime: Long,
    val requestId: String? = null
)

/**
 * Mapper for converting domain dialog data to KAP DialogEnvelope.
 */
object DialogEnvelopeMapper {

    private const val DIALOG_VERSION = "1.2.0"
    private const val INPUT_TYPE_VOICE = "voice"
    private const val STREAM_STATUS_COMPLETED = "completed"

    fun toDialogEnvelope(data: DialogTurnData): DialogEnvelope {
        val userMessage = toUserMessage(data)
        val assistantMessage = toAssistantMessage(data)

        return DialogEnvelope(
            id = data.envelopeId,
            version = DIALOG_VERSION,
            date = data.timestamp,
            data = DialogData(userMessage = userMessage, assistantMessage = assistantMessage)
        )
    }

    private fun toUserMessage(data: DialogTurnData): UserMessage {
        val meta = data.daSessionInfo.meta
        val common = data.daSessionInfo.common

        return UserMessage(
            chatId = data.chatId,
            id = data.userMessageId,
            dateCreated = data.timestamp,
            text = data.inputText,
            userId = meta.userId.takeIfNotBlank(),
            ucpId = meta.ucpId.takeIfNotBlank(),
            sessionId = meta.sessionId.takeIfNotBlank(),
            surface = common.surface.takeIfNotBlank(),
            channel = common.channel.takeIfNotBlank(),
            appSource = common.appSource.takeIfNotBlank(),
            platform = common.platform.takeIfNotBlank(),
            entryPoint = common.entryPoint.takeIfNotBlank(),
            appVersion = common.appVersion.takeIfNotBlank(),
            channelVersion = common.channelVersion.takeIfNotBlank(),
            timeZone = common.timeZone.takeIfNotBlank(),
            inputType = INPUT_TYPE_VOICE,
            previousMessageId = data.previousMessageId
        )
    }

    private fun toAssistantMessage(data: DialogTurnData): AssistantMessage {
        val sessionId = data.daSessionInfo.meta.sessionId

        return AssistantMessage(
            chatId = data.chatId,
            id = data.assistantMessageId,
            dateCreated = data.timestamp,
            text = data.outputText,
            sessionId = sessionId.takeIfNotBlank(),
            previousMessageId = data.userMessageId,
            requestId = data.requestId,
            streamStatus = STREAM_STATUS_COMPLETED,
            agentId = listOf(AgentId(ci = data.agentCi)),
            assistantResponseTime = data.assistantResponseTime
        )
    }

    private fun String.takeIfNotBlank(): String? = takeIf { it.isNotBlank() }
}
