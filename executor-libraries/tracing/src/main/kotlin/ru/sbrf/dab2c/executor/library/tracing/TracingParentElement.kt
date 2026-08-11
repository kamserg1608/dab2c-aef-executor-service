package ru.sbrf.dab2c.executor.library.tracing

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * Session-scoped tracing parent: the root context the session's spans anchor to, and the
 * innermost active span that outbound calls nest under while a tool runs.
 * One instance per session, shared across the session's coroutines.
 */
class TracingParentElement : CoroutineContext.Element {
    override val key get() = Key

    private val rootContext = AtomicReference<Context?>(null)
    private val activeSpan = AtomicReference<Span?>(null)

    /** Anchors the session at its root tracing context. */
    fun setRoot(context: Context) { rootContext.set(context) }

    /** The session's root tracing context to fall back to, or null. */
    fun root(): Context? = rootContext.get()

    /** Makes [span] the parent for subsequently nested outbound spans. */
    fun set(span: Span) { activeSpan.set(span) }

    /** Stops nesting outbound spans under the active span. */
    fun clear() { activeSpan.set(null) }

    /** The active parent span to nest outbound spans under, or null. */
    fun get(): Span? = activeSpan.get()

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<TracingParentElement>
}
