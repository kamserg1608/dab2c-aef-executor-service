package ru.sbrf.dab2c.executor.library.tracing

import io.opentelemetry.api.trace.Span
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Carries a mutable active-span reference across coroutines sharing the same session context.
 * One instance per session — set by the observer layer, read by the facade layer.
 */
class TracingParentElement : CoroutineContext.Element {
    override val key get() = Key

    private val activeSpan = AtomicReference<Span?>(null)

    /** Sets the active parent span for child span creation. */
    fun set(span: Span) { activeSpan.set(span) }

    /** Clears the active parent span. */
    fun clear() { activeSpan.set(null) }

    /** Returns the active parent span, or null. */
    fun get(): Span? = activeSpan.get()

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<TracingParentElement>
}
