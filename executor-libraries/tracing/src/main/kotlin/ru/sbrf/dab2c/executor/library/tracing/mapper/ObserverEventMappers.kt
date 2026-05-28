@file:Suppress("StringLiteralDuplication")

package ru.sbrf.dab2c.executor.library.tracing.mapper

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/** Builds JSON payloads for AEF span attributes. */
object ObserverEventMappers {

    const val EMPTY_OBJECT: String = "{}"

    private val mapper = ObjectMappers.MAPPER

    /** `voice_turn.aef.input` — `{"text": "..."}` or `{}` when text is empty. */
    fun voiceTurnInput(text: String?): String = textObject(text)

    /** `voice_turn.aef.output` — `{"text": "..."}` or `{}` when text is empty. */
    fun voiceTurnOutput(text: String?): String = textObject(text)

    private fun textObject(text: String?): String {
        if (text.isNullOrEmpty()) return EMPTY_OBJECT
        return mapper.writeValueAsString(mapOf("text" to text))
    }

    /** `voice_llm_turn.aef.input` — `{"function_call": {"name": ..., "arguments": <parsed|raw>}}`. */
    fun llmTurnInputFunctionCall(name: String, argumentsJson: String): String {
        val payload = mapper.createObjectNode().apply {
            putObject("function_call").apply {
                put("name", name)
                set<JsonNode>("arguments", parseOrRaw(argumentsJson))
            }
        }
        return mapper.writeValueAsString(payload)
    }

    /** `voice_llm_turn.aef.output` — `{"result": {"content": "<raw>"}, "function_name": "..."}`. */
    fun llmTurnOutputFunctionResult(name: String, content: String): String {
        val payload = mapper.createObjectNode().apply {
            putObject("result").put("content", content)
            put("function_name", name)
        }
        return mapper.writeValueAsString(payload)
    }

    /** `tool.aef.output` — `{"status": "success", "result_available": true}`. */
    fun toolOutput(): String =
        mapper.writeValueAsString(mapOf("status" to "success", "result_available" to true))

    /** `voice_turn.aef.error` — `{"status": int, "message": string}`. */
    fun toVoiceErrorJson(status: Int, message: String): String =
        mapper.writeValueAsString(mapOf("status" to status, "message" to message))

    private fun parseOrRaw(json: String): JsonNode =
        try {
            mapper.readTree(json)
        } catch (ignore: JsonProcessingException) {
            mapper.valueToTree(json) ?: mapper.nullNode()
        }
}
