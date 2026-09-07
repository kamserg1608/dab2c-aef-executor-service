package ru.sbrf.dab2c.executor.clients.giga.agent.tracing

import io.mockk.coEvery
import io.mockk.mockk
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.sbrf.aef.observability.aiservice.AefRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.tracing.aef.AefRequestContextElement
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracingImpl
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacadeImpl

class TracingGigaVoiceAgentClientDecoratorTest {

    private val exporter = InMemorySpanExporter.create()
    private val tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(SimpleSpanProcessor.create(exporter))
        .build()
    private val tracer = OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .build()
        .getTracer("test")
    private val facade: AefTracingFacade = AefTracingFacadeImpl(tracer)
    private val aefTracing = AefHttpOutgoingRequestTracingImpl(facade, ObjectMappers.MAPPER)
    private val delegate: GigaVoiceAgentClient = mockk()
    private val decorator = TracingGigaVoiceAgentClientDecorator(delegate, aefTracing)

    @AfterEach
    fun resetSdkTl() {
        AefRequestContext.clear()
    }

    @Test
    fun `getSettings opens output_request span with request and response bodies`() = runTest {
        coEvery { delegate.getSettings(any(), any(), any(), any()) } returns SettingsResult(
            settings = settings {}, performers = FunctionPerformers(), analytics = emptyList()
        )
        withContext(sessionContext()) {
            decorator.getSettings("conv-1", agentConfiguration(), settings {}, dialogContext())
        }

        val span = exporter.finishedSpanItems.single()
        assertEquals("agent /settings", span.name)
        assertEquals("output_request", span.attributes.get(AttributeKey.stringKey("aef.kind")))
        assertEquals("POST", span.attributes.get(AttributeKey.stringKey("aef.request.method")))
        assertEquals("/settings", span.attributes.get(AttributeKey.stringKey("aef.request.path")))
        assertEquals("200", span.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.request.body")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.response.body")))
        assertEquals("sess-1", span.attributes.get(AttributeKey.stringKey("aef.session_id")))
    }

    @Test
    fun `failed call writes aef_error and 500 status`() = runTest {
        coEvery { delegate.getSettings(any(), any(), any(), any()) } throws RuntimeException("boom")
        withContext(sessionContext()) {
            assertThrows<RuntimeException> {
                decorator.getSettings("conv-1", agentConfiguration(), settings {}, dialogContext())
            }
        }

        val span = exporter.finishedSpanItems.single()
        assertEquals("500", span.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.error")))
    }

    @Test
    fun `postProcess opens output_request span with request and response bodies`() = runTest {
        val expected = PostProcessResult(analytics = emptyList())
        coEvery { delegate.postProcess(any(), any(), any()) } returns expected

        val result = withContext(sessionContext()) {
            decorator.postProcess("conv-1", agentConfiguration(), dialogContext())
        }

        assertSame(expected, result)
        val span = exporter.finishedSpanItems.single()
        assertEquals("agent /postprocess", span.name)
        assertEquals("output_request", span.attributes.get(AttributeKey.stringKey("aef.kind")))
        assertEquals("POST", span.attributes.get(AttributeKey.stringKey("aef.request.method")))
        assertEquals("/postprocess", span.attributes.get(AttributeKey.stringKey("aef.request.path")))
        assertEquals("200", span.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.request.body")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.response.body")))
    }

    @Test
    fun `failed postProcess writes aef_error and 500 status`() = runTest {
        coEvery { delegate.postProcess(any(), any(), any()) } throws RuntimeException("boom")

        withContext(sessionContext()) {
            assertThrows<RuntimeException> {
                decorator.postProcess("conv-1", agentConfiguration(), dialogContext())
            }
        }

        val span = exporter.finishedSpanItems.single()
        assertEquals("500", span.attributes.get(AttributeKey.stringKey("aef.response.status_code")))
        assertNotNull(span.attributes.get(AttributeKey.stringKey("aef.error")))
    }

    @Test
    fun `cancelled postProcess closes the span without an error attribute`() = runTest {
        coEvery { delegate.postProcess(any(), any(), any()) } throws CancellationException("session is gone")

        withContext(sessionContext()) {
            assertThrows<CancellationException> {
                decorator.postProcess("conv-1", agentConfiguration(), dialogContext())
            }
        }

        val span = exporter.finishedSpanItems.single()
        assertEquals("agent /postprocess", span.name)
        assertNull(span.attributes.get(AttributeKey.stringKey("aef.error")))
    }

    private fun dialogContext() = DialogContext(ObjectMappers.MAPPER.createObjectNode())

    private fun sessionContext() =
        HeadersElement(Headers(mapOf("x-session-id" to "sess-1"))) + AefRequestContextElement()

    private fun agentConfiguration() = AgentConfiguration(
        name = "test", type = "voice", functionalSubsystemCi = "ci",
        description = "d", entryPoints = emptyList(),
        ufsServiceAvailable = false, canAccessUserInfo = false,
        toolsMeta = emptyList(), neighboursAgentMeta = emptyList(), toggles = emptyMap()
    )
}
