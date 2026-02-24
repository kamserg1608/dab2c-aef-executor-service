package ru.sbrf.dab2c.executor.library.context

import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element carrying request [Headers].
 */
class HeadersElement(val headers: Headers) : CoroutineContext.Element {
    override val key get() = Key

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<HeadersElement>
}
