package ru.sbrf.dab2c.executor.library.context.mdc

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.reactor.ReactorContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import reactor.util.context.Context

/**
 * Set multiple MDC values and propagate them through coroutine suspensions and reactive code.
 *
 * @param pairs Key-value pairs to set in MDC
 * @param block The suspend block to execute with the MDC values
 * @return The result of the block
 */
suspend fun <T> withMdcValues(vararg pairs: Pair<MdcKey, String>, block: suspend () -> T): T =
    withMdcValues(pairs.toMap(), block)

/**
 * Set multiple MDC values from a map and propagate them through coroutine suspensions and reactive code.
 *
 * @param values Map of MDC key-value pairs to set
 * @param block The suspend block to execute with the MDC values
 * @return The result of the block
 */
suspend fun <T> withMdcValues(values: Map<MdcKey, String>, block: suspend () -> T): T {
    if (values.isEmpty()) return block()

    values.forEach { (key, value) -> RequestContext[key] = value }

    return withReactorContext({ ctx ->
        values.entries.fold(ctx) { acc, (key, value) -> acc.put(key.key, value) }
    }, block)
}

/**
 * Execute a block with the current MDC context preserved across coroutine suspensions.
 *
 * Use this when MDC values are already set (e.g., by a gRPC interceptor) and you
 * need to ensure they're propagated through suspend calls.
 *
 * @param block The suspend block to execute
 * @return The result of the block
 */
suspend fun <T> withMdcContext(block: suspend () -> T): T =
    withReactorContext(RequestContext.asContextModifier(), block)

private suspend fun <T> withReactorContext(
    modifier: (Context) -> Context,
    block: suspend () -> T
): T {
    val currentReactorCtx = currentCoroutineContext()[ReactorContext]?.context ?: Context.empty()
    val updatedReactorCtx = modifier(currentReactorCtx)

    return withContext(MDCContext() + ReactorContext(updatedReactorCtx)) {
        block()
    }
}
