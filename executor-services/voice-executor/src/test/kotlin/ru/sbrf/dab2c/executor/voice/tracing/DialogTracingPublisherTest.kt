package ru.sbrf.dab2c.executor.voice.tracing

import io.grpc.Status
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacadeImpl
import ru.sbrf.dab2c.executor.voice.session.observer.ErrorEmitted
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionCallReceived
import ru.sbrf.dab2c.executor.voice.session.observer.FunctionResultSent
import ru.sbrf.dab2c.executor.voice.session.observer.Replica
import ru.sbrf.dab2c.executor.voice.session.observer.TurnCompleted
import ru.sbrf.dab2c.executor.voice.session.observer.TurnEvent
import ru.sbrf.dab2c.executor.voice.session.observer.VoiceSettings
import ru.sbrf.dab2c.executor.voice.session.observer.WarningEmitted

class DialogTracingPublisherTest {

    private val exporter = InMemorySpanExporter.create()
    private val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build()
    private val tracer = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build().getTracer("test")
    private val facade: AefTracingFacade = AefTracingFacadeImpl(tracer)

    @Test
    fun `OK status when session completes with no cause`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSettingsReceived(testVoiceSettings("vc-1"))
        publisher.onSessionCompleted(cause = null)

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        val session = exporter.finishedSpanItems.single { it.name == "voice session" }
        assertEquals("OK", downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        val settingsJson = session.attributes.get(AttributeKey.stringKey("aef.settings"))
        assertEquals(true, settingsJson!!.contains("\"voiceCallId\":\"vc-1\""))
    }

    @Test
    fun `genuine gRPC error propagates status name`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSessionCompleted(Status.INTERNAL.withDescription("server error").asRuntimeException())

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        assertEquals("INTERNAL", downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
    }

    @Test
    fun `tolerant gRPC termination maps to OK`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSessionCompleted(Status.UNAVAILABLE.asRuntimeException())

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        assertEquals("OK", downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
    }

    @Test
    fun `unexpected EOS downstream close maps to OK`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSessionCompleted(
            Status.INTERNAL
                .withDescription("Received unexpected EOS on empty DATA frame from server")
                .asRuntimeException()
        )

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        assertEquals("OK", downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
    }

    @Test
    fun `unknown throwable is mapped via Status_fromThrowable`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSessionCompleted(IllegalStateException("boom"))

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        val code = downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code"))
        assertEquals(Status.fromThrowable(IllegalStateException()).code.name, code)
    }

    @Test
    fun `single voice_turn carries canonical aef_input aef_output and llm_total_tokens`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(3L)
        publisher.onTurnCompleted(
            turn(user = "hello bot", assistant = "hi user", totalTokens = 42)
        )
        publisher.onSessionCompleted(null)

        val turn = exporter.finishedSpanItems.single { it.name == "voice turn" }
        val llm = exporter.finishedSpanItems.single { it.name == "voice llm turn" }
        assertEquals("""{"text":"hello bot"}""", turn.attributes.get(AttributeKey.stringKey("aef.input")))
        assertEquals("""{"text":"hi user"}""", turn.attributes.get(AttributeKey.stringKey("aef.output")))
        assertEquals("{}", llm.attributes.get(AttributeKey.stringKey("aef.input")))
        assertEquals("{}", llm.attributes.get(AttributeKey.stringKey("aef.output")))
        assertEquals(42L, llm.attributes.get(AttributeKey.longKey("aef.llm.total_tokens")))
    }

    @Test
    fun `warning within turn is written as aef_warning`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(1L)
        publisher.onTurnCompleted(
            turn(assistant = "ok", events = listOf(WarningEmitted("rate-limit-soft", 2L)))
        )
        publisher.onSessionCompleted(null)

        val turn = exporter.finishedSpanItems.single { it.name == "voice turn" }
        assertEquals("rate-limit-soft", turn.attributes.get(AttributeKey.stringKey("aef.warning")))
    }

    @Test
    fun `error within turn synthesises ERROR session status when cause is null`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(1L)
        publisher.onErrorEmitted(ErrorEmitted(500, "boom", 2L))
        publisher.onTurnCompleted(
            turn(events = listOf(ErrorEmitted(500, "boom", 2L)))
        )
        publisher.onSessionCompleted(null)

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        assertEquals("ERROR", downstream.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        val turn = exporter.finishedSpanItems.single { it.name == "voice turn" }
        val errorJson = turn.attributes.get(AttributeKey.stringKey("aef.error"))
        assertEquals(true, errorJson!!.contains("\"status\":500"))
        assertEquals(true, errorJson.contains("\"message\":\"boom\""))
    }

    @Test
    fun `tool span carries canonical aef_input and aef_output objects`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(3L)
        publisher.onFunctionCallReceived(
            FunctionCallReceived(name = "find_bank_office", arguments = """{"city":"Moscow"}""", atMs = 4L)
        )
        publisher.onFunctionResultSent(
            FunctionResultSent(name = "find_bank_office", content = """{"office":"123"}""", atMs = 5L)
        )
        publisher.onTurnCompleted(turn(user = "how is weather", assistant = "calling"))
        publisher.onSessionCompleted(null)

        val tool = exporter.finishedSpanItems.single { it.name == "find_bank_office" }
        assertEquals("tool", tool.attributes.get(AttributeKey.stringKey("aef.kind")))
        val input = tool.attributes.get(AttributeKey.stringKey("aef.input"))
        assertNotNull(input)
        assertEquals(true, input!!.contains("\"name\":\"find_bank_office\""))
        assertEquals(true, input.contains("\"arguments\""))
        assertEquals(true, input.contains("\"city\""))
        val output = tool.attributes.get(AttributeKey.stringKey("aef.output"))
        assertNotNull(output)
        assertEquals(true, output!!.contains("\"status\":\"success\""))
    }

    @Test
    fun `voice_llm_turn carries function_call input and function_result output`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(3L)
        publisher.onFunctionCallReceived(FunctionCallReceived("weather", """{"city":"Moscow"}""", 4L))
        publisher.onFunctionResultSent(FunctionResultSent("weather", """{"forecast":"sunny"}""", 5L))
        publisher.onTurnCompleted(
            turn(
                user = "how is weather",
                assistant = "done",
                events = listOf(
                    FunctionCallReceived("weather", """{"city":"Moscow"}""", 4L),
                    FunctionResultSent("weather", """{"forecast":"sunny"}""", 5L),
                )
            )
        )
        publisher.onSessionCompleted(null)

        val llm = exporter.finishedSpanItems.single { it.name == "voice llm turn" }
        val input = llm.attributes.get(AttributeKey.stringKey("aef.input"))
        assertEquals(true, input!!.contains("\"function_call\":{"))
        assertEquals(true, input.contains("\"name\":\"weather\""))
        val output = llm.attributes.get(AttributeKey.stringKey("aef.output"))
        assertEquals(true, output!!.contains("\"result\":{\"content\":\"{\\\"forecast\\\":\\\"sunny\\\"}\"}"))
        assertEquals(true, output.contains("\"function_name\":\"weather\""))
    }

    @Test
    fun `downstream span carries correct gRPC method path`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSettingsReceived(testVoiceSettings("vc-path"))
        publisher.onSessionCompleted(cause = null)

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream gigavoice stream" }
        assertEquals(
            "GigaVoiceProtocol.GigaVoiceService/GigaVoice",
            downstream.attributes.get(AttributeKey.stringKey("aef.request.path"))
        )
    }

    @Test
    fun `aef_settings contains full settings data, not just voice_call_id`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onSettingsReceived(testVoiceSettings("vc-full"))
        publisher.onSessionCompleted(cause = null)

        val session = exporter.finishedSpanItems.single { it.name == "voice session" }
        val settingsJson = session.attributes.get(AttributeKey.stringKey("aef.settings"))!!
        assertEquals(true, settingsJson.contains("\"voiceCallId\":\"vc-full\""))
        assertEquals(true, settingsJson.contains("\"mode\":\"RECOGNIZE_GIGACHAT_SYNTHESIS\""))
    }

    @Test
    fun `multiple warnings are concatenated on both voice_turn and voice_llm_turn`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(1L)
        publisher.onTurnCompleted(
            turn(
                assistant = "partial",
                events = listOf(
                    WarningEmitted("rate-limit", 2L),
                    WarningEmitted("quota-low", 3L),
                    ErrorEmitted(503, "overloaded", 4L),
                )
            )
        )
        publisher.onSessionCompleted(null)

        val turn = exporter.finishedSpanItems.single { it.name == "voice turn" }
        assertEquals("rate-limit; quota-low", turn.attributes.get(AttributeKey.stringKey("aef.warning")))
        val turnError = turn.attributes.get(AttributeKey.stringKey("aef.error"))
        assertEquals(true, turnError!!.contains("\"status\":503"))

        val llm = exporter.finishedSpanItems.single { it.name == "voice llm turn" }
        assertEquals("rate-limit; quota-low", llm.attributes.get(AttributeKey.stringKey("aef.warning")))
        val llmError = llm.attributes.get(AttributeKey.stringKey("aef.error"))
        assertEquals(true, llmError!!.contains("\"status\":503"))
    }

    @Test
    fun `span hierarchy is correct for single turn`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(3L)
        publisher.onTurnCompleted(turn(user = "hi", assistant = "hello"))
        publisher.onSessionCompleted(null)

        assertParentChild("voice session", "downstream gigavoice stream")
        assertParentChild("voice turn", "voice session")
        assertParentChild("voice llm turn", "voice turn")
    }

    @Test
    fun `tool span is nested under voice_llm_turn`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(3L)
        publisher.onFunctionCallReceived(FunctionCallReceived("get_balance", "{}", 4L))
        publisher.onFunctionResultSent(FunctionResultSent("get_balance", "{}", 5L))
        publisher.onTurnCompleted(turn(user = "balance", assistant = "done"))
        publisher.onSessionCompleted(null)

        assertParentChild("get_balance", "voice llm turn")
        assertParentChild("voice llm turn", "voice turn")
        assertParentChild("voice turn", "voice session")
    }

    @Test
    fun `function_call after a closed assistant segment still nests tool under voice_llm_turn`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onAssistantReplicaStarted(1L)
        publisher.onAssistantReplicaCompleted(Replica("", 1L, 2L))
        publisher.onFunctionCallReceived(FunctionCallReceived("get_sbol_info", "{}", 3L))
        publisher.onFunctionResultSent(FunctionResultSent("get_sbol_info", "{}", 4L))
        publisher.onTurnCompleted(
            turn(
                assistant = "Не получилось проверить информацию",
                events = listOf(
                    FunctionCallReceived("get_sbol_info", "{}", 3L),
                    FunctionResultSent("get_sbol_info", "{}", 4L),
                )
            )
        )
        publisher.onSessionCompleted(null)

        val tool = exporter.finishedSpanItems.single { it.name == "get_sbol_info" }
        val llmTurns = exporter.finishedSpanItems.filter { it.name == "voice llm turn" }
        assertEquals(1, llmTurns.size)
        assertEquals(llmTurns.single().spanId, tool.parentSpanId, "tool must nest under voice_llm_turn, not the root")
        assertParentChild("voice llm turn", "voice turn")
        assertParentChild("voice turn", "voice session")
    }

    @Test
    fun `empty TurnCompleted does not emit a voice_turn`() = runTest {
        val publisher = DialogTracingPublisher(facade)
        publisher.onSessionStarted()
        publisher.onTurnCompleted(TurnCompleted(null, null, emptyList(), null))
        publisher.onSessionCompleted(null)

        assertTrue(exporter.finishedSpanItems.none { it.name == "voice turn" })
        assertTrue(exporter.finishedSpanItems.none { it.name == "voice llm turn" })
    }

    @Test
    fun `onFunctionCallReceived sets and onFunctionResultSent clears TracingParentElement`() = runTest {
        val element = TracingParentElement()
        withContext(element) {
            val publisher = DialogTracingPublisher(facade)
            publisher.onSessionStarted()
            publisher.onAssistantReplicaStarted(1L)

            assertNull(element.get())
            publisher.onFunctionCallReceived(FunctionCallReceived("get_balance", "{}", 2L))
            assertNotNull(element.get())

            publisher.onFunctionResultSent(FunctionResultSent("get_balance", "{}", 3L))
            assertNull(element.get())

            publisher.onTurnCompleted(turn(assistant = "done"))
            publisher.onSessionCompleted(null)
        }
    }

    @Test
    fun `onSessionCompleted clears TracingParentElement when tool is dangling`() = runTest {
        val element = TracingParentElement()
        withContext(element) {
            val publisher = DialogTracingPublisher(facade)
            publisher.onSessionStarted()
            publisher.onAssistantReplicaStarted(1L)
            publisher.onFunctionCallReceived(FunctionCallReceived("get_balance", "{}", 2L))

            assertNotNull(element.get())
            publisher.onSessionCompleted(null)
            assertNull(element.get())
        }
    }

    private fun assertParentChild(childName: String, parentName: String) {
        val spans = exporter.finishedSpanItems
        val child = spans.single { it.name == childName }
        val parent = spans.single { it.name == parentName }
        assertEquals(
            parent.spanId, child.parentSpanId,
            "Expected '$childName' to be child of '$parentName'"
        )
    }

    private fun turn(
        user: String? = null,
        assistant: String? = null,
        events: List<TurnEvent> = emptyList(),
        totalTokens: Int? = null,
    ) = TurnCompleted(
        userReplica = user?.let { Replica(it, 1L, 2L) },
        assistantReplica = assistant?.let { Replica(it, 3L, 4L) },
        turnEvents = events,
        totalTokens = totalTokens,
    )

    private fun testVoiceSettings(voiceCallId: String) = VoiceSettings(
        settingsData = mapOf(
            "voiceCallId" to voiceCallId,
            "mode" to "RECOGNIZE_GIGACHAT_SYNTHESIS",
        )
    )
}
