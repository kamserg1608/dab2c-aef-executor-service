package ru.sbrf.dab2c.executor.library.tracing.propagation

import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class W3CTraceContextInjectorTest {

    private val tracer = OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder().build())
        .build()
        .getTracer("test")

    @Test
    fun `injects traceparent matching the span context`() {
        val span = tracer.spanBuilder("s").startSpan()
        val headers = mutableMapOf<String, String>()

        W3CTraceContextInjector.inject(Context.root().with(span)) { key, value -> headers[key] = value }
        span.end()

        val ctx = span.spanContext
        assertTrue(headers.containsKey("traceparent"))
        assertEquals("00-${ctx.traceId}-${ctx.spanId}-${ctx.traceFlags.asHex()}", headers["traceparent"])
    }

    @Test
    fun `does not inject traceparent for an invalid context`() {
        val headers = mutableMapOf<String, String>()

        W3CTraceContextInjector.inject(Context.root()) { key, value -> headers[key] = value }

        assertFalse(headers.containsKey("traceparent"))
    }
}
