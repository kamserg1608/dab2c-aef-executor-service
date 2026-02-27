package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Represents an agent identifier with its CI (Configuration Item).
 */
data class AgentId(
    val ci: String
)

/**
 * Represents an assistant message in a dialog for the KAP dialogs topic.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AssistantMessage(
    val chatId: String,
    val id: String,
    val dateCreated: Long,
    val text: String,
    val isMine: Boolean = false,
    val sessionId: String? = null,
    val previousMessageId: String? = null,
    val requestId: String? = null,
    val streamStatus: String? = null,
    val agentId: List<AgentId>? = null,
    val assistantResponseTime: Long? = null,
    val isIdp: Boolean = false
)
