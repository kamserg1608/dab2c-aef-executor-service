package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Represents a user message in a dialog for the KAP dialogs topic.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserMessage(
    val chatId: String,
    val id: String,
    val dateCreated: Long,
    val text: String,
    val isMine: Boolean = true,
    val userId: String? = null,
    val ucpId: String? = null,
    val sessionId: String? = null,
    val surface: String? = null,
    val channel: String? = null,
    val appSource: String? = null,
    val platform: String? = null,
    val entryPoint: String? = null,
    val appVersion: String? = null,
    val channelVersion: String? = null,
    val timeZone: String? = null,
    val inputType: String? = null,
    val previousMessageId: String? = null,
    val dateModified: Long? = null,
    val requestId: String? = null
)
