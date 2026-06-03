package ru.sbrf.dab2c.executor.voice.exception

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.CancellationException

/**
 * Centralized registry of exceptions that indicate a normal (non-error) session termination.
 *
 * Tolerant exceptions are treated as successful completion: no failed audit event is emitted
 * and no stacktrace is logged.
 */
object TolerantExceptionRegistry {

    private const val NORMAL_CLOSE_MARKER = "unexpected EOS on empty DATA frame"

    private val entries: List<TolerantEntry> = listOf(
        TolerantEntry(CancellationException::class.java),
        TolerantEntry(StatusException::class.java, Status.Code.UNAVAILABLE),
        TolerantEntry(StatusException::class.java, Status.Code.CANCELLED),
        TolerantEntry(StatusRuntimeException::class.java, Status.Code.UNAVAILABLE),
        TolerantEntry(StatusRuntimeException::class.java, Status.Code.CANCELLED),
        TolerantEntry(StatusException::class.java, descriptionContains = NORMAL_CLOSE_MARKER),
        TolerantEntry(StatusRuntimeException::class.java, descriptionContains = NORMAL_CLOSE_MARKER),
    )

    /** Returns true if the exception indicates a normal session termination. */
    fun isTolerant(cause: Throwable): Boolean = entries.any { matches(it, cause) }

    private fun matches(entry: TolerantEntry, cause: Throwable): Boolean {
        val codeOk = entry.statusCode == null || extractGrpcCode(cause) == entry.statusCode
        val descOk = entry.descriptionContains == null ||
            extractDescription(cause)?.contains(entry.descriptionContains, ignoreCase = true) == true
        return entry.exceptionClass.isInstance(cause) && codeOk && descOk
    }

    /** Extracts a human-readable error code: gRPC status name or exception class name. */
    fun extractErrorCode(cause: Throwable): String = when (cause) {
        is StatusException -> cause.status.code.name
        is StatusRuntimeException -> cause.status.code.name
        else -> cause::class.simpleName ?: "UNKNOWN"
    }

    private fun extractGrpcCode(cause: Throwable): Status.Code? = when (cause) {
        is StatusException -> cause.status.code
        is StatusRuntimeException -> cause.status.code
        else -> null
    }

    private fun extractDescription(cause: Throwable): String? = when (cause) {
        is StatusException -> cause.status.description
        is StatusRuntimeException -> cause.status.description
        else -> null
    }

    private data class TolerantEntry(
        val exceptionClass: Class<out Throwable>,
        val statusCode: Status.Code? = null,
        val descriptionContains: String? = null
    )
}
