package ru.sbrf.dab2c.executor.voice.logging

import kotlinx.coroutines.CancellationException

/** How a flow/stream ended, derived from its terminal cause. `CANCELLED` is not a failure. */
enum class CompletionOutcome { NORMAL, CANCELLED, FAILED }

/** Classifies a flow completion cause: null → NORMAL, cancellation → CANCELLED, else FAILED. */
fun completionOutcome(cause: Throwable?): CompletionOutcome = when {
    cause == null -> CompletionOutcome.NORMAL
    cause is CancellationException -> CompletionOutcome.CANCELLED
    else -> CompletionOutcome.FAILED
}
