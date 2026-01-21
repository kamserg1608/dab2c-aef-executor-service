package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Envelope for dialog messages sent to the KAP dialogs topic.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DialogEnvelope(
    val id: String,
    val version: String,
    val date: Long,
    val data: DialogData
)

/**
 * Contains the user and assistant messages in a dialog.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DialogData(
    val userMessage: UserMessage,
    val assistantMessage: AssistantMessage? = null
)
