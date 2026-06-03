package ru.sbrf.dab2c.executor.library.tracing.propagation

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.TextMapSetter

/** Injects W3C trace-context (`traceparent`/`tracestate`) headers from an OTel [Context]. */
object W3CTraceContextInjector {

    private val propagator = W3CTraceContextPropagator.getInstance()

    private val setter = TextMapSetter<(String, String) -> Unit> { carrier, key, value ->
        carrier?.invoke(key, value)
    }

    /** Writes trace-context headers for [context] via [setHeader]; no-op for an invalid span context. */
    fun inject(context: Context, setHeader: (String, String) -> Unit) {
        propagator.inject(context, setHeader, setter)
    }
}
