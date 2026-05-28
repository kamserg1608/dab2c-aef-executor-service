package ru.sbrf.dab2c.executor.library.tracing.grpc

import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Bridges OTel Context captured on the gRPC server thread into a coroutine context element.
 * Fail-open: returns [EmptyCoroutineContext] when no captured context is found or any error occurs.
 */
object OtelGrpcBridge {

    internal val OTEL_CTX_KEY: io.grpc.Context.Key<Context> =
        io.grpc.Context.key("aef-otel-context")

    /** Coroutine context element carrying the captured OTel Context, or no-op if unavailable. */
    fun coroutineContextElement(): CoroutineContext =
        try {
            OTEL_CTX_KEY.get()?.asContextElement() ?: EmptyCoroutineContext
        } catch (e: Exception) {
            EmptyCoroutineContext
        }
}
