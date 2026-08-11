package ru.sbrf.dab2c.executor.library.common

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching], but always rethrows [CancellationException] so coroutine
 * cancellation semantics are preserved. Any other [Exception] is wrapped in
 * [Result.failure] for the caller to handle.
 */
inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
