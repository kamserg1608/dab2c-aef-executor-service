package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/** Extra data attached to a dialog turn — events and message boundary timestamps. */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class DialogTurnExtra(
    val events: List<DialogTurnEvent> = emptyList(),
    val userMessageStartTS: Long? = null,
    val userMessageEndTS: Long? = null,
    val assistantMessageStartTS: Long? = null,
    val assistantMessageEndTS: Long? = null
)

/** A single event captured during a dialog turn. */
data class DialogTurnEvent(
    val eventName: String,
    val timestamp: Long,
    val payload: EventPayload
)

/** Payload variants for dialog turn events. */
sealed interface EventPayload

/** Payload for function_call events. */
data class FunctionCallPayload(
    @JsonProperty("function_call")
    val functionCall: FunctionCallDetails,
    val timestamp: Long
) : EventPayload

/** Details of a function call invocation. */
data class FunctionCallDetails(
    val name: String,
    val arguments: String
)

/** Payload for function_result events. */
data class FunctionResultPayload(
    val content: String,
    @JsonProperty("function_name")
    val functionName: String
) : EventPayload

/** Payload for warning events. */
data class WarningPayload(
    val message: String
) : EventPayload

/** Payload for error events. */
data class ErrorPayload(
    val status: Int,
    val message: String
) : EventPayload
