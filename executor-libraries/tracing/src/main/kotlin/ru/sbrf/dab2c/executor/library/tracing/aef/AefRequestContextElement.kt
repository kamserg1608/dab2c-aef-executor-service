package ru.sbrf.dab2c.executor.library.tracing.aef

import kotlinx.coroutines.ThreadContextElement
import ru.sbrf.aef.observability.aiservice.AefRequestContext
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import kotlin.coroutines.CoroutineContext

/**
 * Bridges incoming-call headers (from [HeadersElement]) into the SDK's
 * [AefRequestContext] ThreadLocal on every coroutine resumption.
 *
 * Headers are passed through unchanged — the SDK is responsible for picking up
 * whatever keys it expects (e.g. `x-session-id`).
 */
class AefRequestContextElement : ThreadContextElement<Map<String, String>?> {

    override val key: CoroutineContext.Key<*> get() = Key

    override fun updateThreadContext(context: CoroutineContext): Map<String, String>? {
        val prev = AefRequestContext.getHeaders()
        val headers = context[HeadersElement]?.headers
        if (headers.isNullOrEmpty()) {
            AefRequestContext.clear()
        } else {
            AefRequestContext.setHeaders(headers)
        }
        return prev
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: Map<String, String>?) {
        if (oldState.isNullOrEmpty()) {
            AefRequestContext.clear()
        } else {
            AefRequestContext.setHeaders(oldState)
        }
    }

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<AefRequestContextElement>
}
