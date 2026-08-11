package ru.sbrf.dab2c.executor.library.tracing.grpc

import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class OtelContextCapturingInterceptorTest {

    private val openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(SdkTracerProvider.builder().build())
        .build()
    private val tracer = openTelemetry.getTracer("test")

    @Test
    fun `interceptor captures Context_current as gRPC context value`() {
        val span = tracer.spanBuilder("seed").startSpan()
        val seededOtel = Context.current().with(span)

        val interceptor = OtelContextCapturingInterceptor()
        val capturedHolder = arrayOfNulls<Context>(1)

        val handler = ServerCallHandler<Any, Any> { _, _ ->
            capturedHolder[0] = OtelGrpcBridge.OTEL_CTX_KEY.get()
            object : ServerCall.Listener<Any>() {}
        }

        seededOtel.makeCurrent().use {
            @Suppress("UNCHECKED_CAST")
            val call = NoopServerCall<Any, Any>() as ServerCall<Any, Any>
            interceptor.interceptCall(call, Metadata(), handler)
        }

        assertNotNull(capturedHolder[0])
        assertSame(span, Span.fromContext(capturedHolder[0]!!))
        span.end()
    }

    @Test
    fun `OtelGrpcBridge yields EmptyCoroutineContext outside grpc scope`() = runTest {
        val element = OtelGrpcBridge.coroutineContextElement()
        withContext(element) {
            assertEquals(Context.root(), Context.current())
        }
    }

    private class NoopServerCall<ReqT, RespT> : ServerCall<ReqT, RespT>() {
        override fun request(numMessages: Int) {}
        override fun sendHeaders(headers: Metadata) {}
        override fun sendMessage(message: RespT) {}
        override fun close(status: io.grpc.Status, trailers: Metadata) {}
        override fun isCancelled(): Boolean = false
        override fun getMethodDescriptor(): io.grpc.MethodDescriptor<ReqT, RespT> =
            error("not used")
    }
}
