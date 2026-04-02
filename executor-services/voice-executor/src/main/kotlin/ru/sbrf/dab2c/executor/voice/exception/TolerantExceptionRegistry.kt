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

    private val entries: List<TolerantEntry> = listOf(
        TolerantEntry(CancellationException::class.java),
        TolerantEntry(StatusException::class.java, Status.Code.UNAVAILABLE),
        TolerantEntry(StatusException::class.java, Status.Code.CANCELLED),
        TolerantEntry(StatusRuntimeException::class.java, Status.Code.UNAVAILABLE),
        TolerantEntry(StatusRuntimeException::class.java, Status.Code.CANCELLED),
    )

    /** Returns true if the exception indicates a normal session termination. */
    fun isTolerant(cause: Throwable): Boolean = entries.any { entry ->
        entry.exceptionClass.isInstance(cause) &&
            (entry.statusCode == null || extractGrpcCode(cause) == entry.statusCode)
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

    private data class TolerantEntry(
        val exceptionClass: Class<out Throwable>,
        val statusCode: Status.Code? = null
    )
}
