package ru.sbrf.dab2c.executor.library.tracing.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

class ObserverEventMappersTest {

    private val mapper = ObjectMappers.MAPPER

    @Test
    fun `voiceTurnInput wraps text into object or returns empty`() {
        assertEquals("""{"text":"hello"}""", ObserverEventMappers.voiceTurnInput("hello"))
        assertEquals("{}", ObserverEventMappers.voiceTurnInput(null))
        assertEquals("{}", ObserverEventMappers.voiceTurnInput(""))
    }

    @Test
    fun `voiceTurnOutput wraps text into object or returns empty`() {
        assertEquals("""{"text":"world"}""", ObserverEventMappers.voiceTurnOutput("world"))
        assertEquals("{}", ObserverEventMappers.voiceTurnOutput(null))
    }

    @Test
    fun `llmTurnInputFunctionCall produces nested function_call with parsed arguments`() {
        val json = ObserverEventMappers.llmTurnInputFunctionCall(
            name = "weather",
            argumentsJson = """{"city":"Moscow","limit":3}"""
        )
        val tree = mapper.readTree(json)
        val fc = tree["function_call"]
        assertEquals("weather", fc["name"].asText())
        assertEquals("Moscow", fc["arguments"]["city"].asText())
        assertEquals(3, fc["arguments"]["limit"].asInt())
    }

    @Test
    fun `llmTurnInputFunctionCall falls back to raw string on bad JSON`() {
        val json = ObserverEventMappers.llmTurnInputFunctionCall("f", "not-json{")
        val tree = mapper.readTree(json)
        assertEquals("not-json{", tree["function_call"]["arguments"].asText())
    }

    @Test
    fun `llmTurnOutputFunctionResult wraps content under result and adds function_name`() {
        val json = ObserverEventMappers.llmTurnOutputFunctionResult("weather", """{"ok":true}""")
        val tree = mapper.readTree(json)
        assertEquals("""{"ok":true}""", tree["result"]["content"].asText())
        assertEquals("weather", tree["function_name"].asText())
    }

    @Test
    fun `toolOutput emits status and result_available`() {
        val tree = mapper.readTree(ObserverEventMappers.toolOutput())
        assertEquals("success", tree["status"].asText())
        assertEquals(true, tree["result_available"].asBoolean())
    }

    @Test
    fun `toVoiceErrorJson produces status and message fields`() {
        val json = ObserverEventMappers.toVoiceErrorJson(404, "not found")
        val tree = mapper.readTree(json)
        assertEquals(404, tree["status"].asInt())
        assertEquals("not found", tree["message"].asText())
    }
}
