package ru.sbrf.dab2c.executor.library.tracing.facade

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sbrf.aef.observability.aiservice.AefRequestContext
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.tracing.TracingParentElement
import ru.sbrf.dab2c.executor.library.tracing.aef.AefRequestContextElement

class AefTracingFacadeTest {

    private val exporter = InMemorySpanExporter.create()
    private val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build()
    private val tracer = OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build().getTracer("test")
    private val facade: AefTracingFacade = AefTracingFacadeImpl(tracer)

    @AfterEach
    fun resetSdkTl() {
        AefRequestContext.clear()
    }

    @Test
    fun `endVoiceTurn writes canonical aef_input and aef_output objects`() = runTest {
        withContext(sessionContext("sess-1")) {
            val span = facade.startVoiceTurn("voice turn")
            facade.endVoiceTurn(
                span,
                VoiceTurnOutput(
                    inputJson = """{"text":"hello"}""",
                    outputJson = """{"text":"world"}""",
                    warning = null,
                    error = null,
                )
            )
        }
        val data = exporter.finishedSpanItems.single()
        assertEquals("sess-1", data.attributes.get(AttributeKey.stringKey("aef.session_id")))
        assertEquals("voice_turn", data.attributes.get(AttributeKey.stringKey("aef.kind")))
        assertEquals("""{"text":"hello"}""", data.attributes.get(AttributeKey.stringKey("aef.input")))
        assertEquals("""{"text":"world"}""", data.attributes.get(AttributeKey.stringKey("aef.output")))
    }

    @Test
    fun `endVoiceTurn converts unicode escapes to characters in aef_output`() = runTest {
        val span = facade.startVoiceTurn("voice turn")
        facade.endVoiceTurn(
            span,
            VoiceTurnOutput(
                inputJson = "{}",
                outputJson = """{"text":"\u041F\u0440\u0438\u0432\u0435\u0442"}""",
                warning = null,
                error = null,
            )
        )

        val output = exporter.finishedSpanItems.single()
            .attributes
            .get(AttributeKey.stringKey("aef.output"))
        assertEquals("""{"text":"Привет"}""", output)
    }

    @Test
    fun `startVoiceTurn without session element leaves session_id empty`() = runTest {
        val span = facade.startVoiceTurn("voice turn")
        facade.endVoiceTurn(span, VoiceTurnOutput("{}", "{}", null, null))

        val data = exporter.finishedSpanItems.single()
        assertNull(data.attributes.get(AttributeKey.stringKey("aef.session_id")))
    }

    @Test
    fun `endVoiceLlmTurn writes input output and tokens`() = runTest {
        val span = facade.startVoiceLlmTurn("voice llm turn")
        facade.endVoiceLlmTurn(span, VoiceLlmTurnOutput("{}", "{}", totalTokens = 42L, null, null))

        val data = exporter.finishedSpanItems.single()
        assertEquals("voice_llm_turn", data.attributes.get(AttributeKey.stringKey("aef.kind")))
        assertEquals("{}", data.attributes.get(AttributeKey.stringKey("aef.input")))
        assertEquals("{}", data.attributes.get(AttributeKey.stringKey("aef.output")))
        assertEquals(42L, data.attributes.get(AttributeKey.longKey("aef.llm.total_tokens")))
    }

    @Test
    fun `tool span writes aef_input at start and aef_output at end`() = runTest {
        val span = facade.startTool(
            "find_office",
            """{"city":"\u041C\u043E\u0441\u043A\u0432\u0430"}"""
        )
        facade.endTool(span, """{"status":"success","result_available":true}""")

        val data = exporter.finishedSpanItems.single()
        assertEquals("tool", data.attributes.get(AttributeKey.stringKey("aef.kind")))
        val input = data.attributes.get(AttributeKey.stringKey("aef.input"))
        assertNotNull(input)
        assertEquals(true, input!!.contains("\"name\":\"find_office\""))
        assertEquals(true, input.contains("\"arguments\""))
        assertEquals(true, input.contains("\"city\":\"Москва\""))
        assertEquals(
            """{"status":"success","result_available":true}""",
            data.attributes.get(AttributeKey.stringKey("aef.output"))
        )
    }

    @Test
    fun `startHttpOutputRequest filters sensitive headers`() = runTest {
        withContext(sessionContext("sess-x")) {
            val span = facade.startHttpOutputRequest(
                spanName = "agent /settings",
                method = "POST",
                path = "/settings",
                requestBody = "{\"k\":\"v\"}",
                requestHeaders = mapOf(
                    "Content-Type" to listOf("application/json"),
                    "Authorization" to listOf("Bearer secret-token")
                )
            )
            facade.endHttpOutputRequest(
                span, 200, "{\"ok\":true}",
                mapOf("Server" to listOf("nginx"))
            )
        }

        val data = exporter.finishedSpanItems.single()
        val headersJson = data.attributes.get(AttributeKey.stringKey("aef.request.headers"))
        assertNotNull(headersJson)
        assertEquals(false, headersJson!!.contains("Authorization"))
        assertEquals(true, headersJson.contains("Content-Type"))
        assertEquals("200", data.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        assertEquals("{\"ok\":true}", data.attributes.get(AttributeKey.stringKey("aef.response.body")))
    }

    @Test
    fun `endVoiceLlmTurn writes warning and error when provided`() = runTest {
        val span = facade.startVoiceLlmTurn("voice llm turn")
        facade.endVoiceLlmTurn(
            span,
            VoiceLlmTurnOutput(
                inputJson = "{}",
                outputJson = "{}",
                totalTokens = null,
                warning = "soft-limit; hard-limit",
                error = """{"status":503,"message":"overloaded"}""",
            )
        )

        val data = exporter.finishedSpanItems.single()
        assertEquals("soft-limit; hard-limit", data.attributes.get(AttributeKey.stringKey("aef.warning")))
        assertEquals(
            """{"status":503,"message":"overloaded"}""",
            data.attributes.get(AttributeKey.stringKey("aef.error"))
        )
    }

    @Test
    fun `startHttpOutputRequest nests under active span from TracingParentElement`() = runTest {
        val element = TracingParentElement()
        withContext(sessionContext("sess-parent") + element) {
            val parentSpan = facade.startTool("parent_tool", "{}")
            element.set(parentSpan)

            val httpSpan = facade.startHttpOutputRequest(
                "agent /functions", "POST", "/functions", "{}", emptyMap()
            )
            facade.endHttpOutputRequest(httpSpan, 200, "{}", emptyMap())
            facade.endTool(parentSpan, "{}")
        }

        val httpData = exporter.finishedSpanItems.single { it.name == "agent /functions" }
        val toolData = exporter.finishedSpanItems.single { it.name == "parent_tool" }
        assertEquals(toolData.spanId, httpData.parentSpanId)
    }

    @Test
    fun `startDownstreamGrpcOutputRequest parents under root from TracingParentElement`() = runTest {
        val root = tracer.spanBuilder("agent start").startSpan()
        val element = TracingParentElement().apply { setRoot(Context.root().with(root)) }
        withContext(sessionContext("sess-root") + element) {
            val span = facade.startDownstreamGrpcOutputRequest("downstream", "/svc", emptyMap<String, Any>())
            facade.endDownstreamGrpcOutputRequest(span, "OK")
        }
        root.end()

        val downstream = exporter.finishedSpanItems.single { it.name == "downstream" }
        val agentStart = exporter.finishedSpanItems.single { it.name == "agent start" }
        assertEquals(agentStart.spanId, downstream.parentSpanId)
    }

    @Test
    fun `startHttpOutputRequest uses ambient context when no TracingParentElement`() = runTest {
        withContext(sessionContext("sess-no-parent")) {
            val httpSpan = facade.startHttpOutputRequest(
                "agent /settings", "POST", "/settings", "{}", emptyMap()
            )
            facade.endHttpOutputRequest(httpSpan, 200, "{}", emptyMap())
        }
        val data = exporter.finishedSpanItems.single()
        assertEquals("agent /settings", data.name)
    }

    private fun sessionContext(sessionId: String) =
        HeadersElement(Headers(mapOf("x-session-id" to sessionId))) + AefRequestContextElement()
}
