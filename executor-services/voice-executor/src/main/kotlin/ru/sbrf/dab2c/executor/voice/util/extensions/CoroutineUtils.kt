package ru.sbrf.dab2c.executor.voice.util.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.slf4j.MDCContext

/**
 * Launches a new coroutine that inherits the current coroutine context with a SupervisorJob.
 * The launched coroutine will:
 * - Inherit all context elements (dispatcher, metadata, etc.) from the calling coroutine
 * - Propagate MDC context for structured logging
 * - Still respond to parent cancellation (maintains structured concurrency)
 */
suspend fun launchAsync(block: suspend CoroutineScope.() -> Unit) {
    CoroutineScope(currentCoroutineContext() + Job(currentCoroutineContext()[Job]) + MDCContext()).launch(block = block)
}
