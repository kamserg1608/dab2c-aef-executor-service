package ru.sbrf.dab2c.executor.library.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

/** Coroutine scope that its owner cancels explicitly. */
class CloseableCoroutineScope(context: CoroutineContext) : CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    /** Cancels every coroutine still running in this scope. */
    fun cancel() = coroutineContext.cancel()
}
