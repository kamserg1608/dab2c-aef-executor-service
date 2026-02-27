package ru.sbrf.dab2c.executor.library.monitoring.service.api

import java.io.IOException
import java.util.concurrent.TimeoutException

/** Descriptor for an HTTP call to be monitored. */
data class HttpCallDescriptor(
    val destinationService: String,
    val endpoint: String,
    val method: String = "post"
)

/** Record timer and counter metrics around an HTTP call. */
suspend fun <T> MetricFactory.monitorHttpCall(
    timerMetric: Metric,
    counterMetric: Metric,
    descriptor: HttpCallDescriptor,
    block: suspend () -> T
): T {
    val baseTags = mapOf(
        MetricTags.DESTINATION_SERVICE to descriptor.destinationService,
        MetricTags.ENDPOINT to descriptor.endpoint,
        MetricTags.METHOD to descriptor.method
    )

    val startTime = System.nanoTime()
    var statusCode = STATUS_OK
    try {
        return block()
    } catch (e: Exception) {
        statusCode = classifyException(e)
        throw e
    } finally {
        val durationNs = System.nanoTime() - startTime
        val allTags = baseTags + (MetricTags.STATUS_CODE to statusCode)
        recordDuration(timerMetric, durationNs, allTags)
        incrementCounter(counterMetric, allTags)
    }
}

private fun classifyException(e: Throwable): String = when {
    e.isTimeoutException() -> STATUS_TIMEOUT
    e is IOException -> STATUS_NETWORK_ERROR
    else -> STATUS_EXCEPTION
}

private fun Throwable.isTimeoutException(): Boolean =
    this is TimeoutException ||
        this::class.simpleName.orEmpty().contains("Timeout", ignoreCase = true)

private const val STATUS_OK = "200"
private const val STATUS_TIMEOUT = "timeout"
private const val STATUS_NETWORK_ERROR = "network_error"
private const val STATUS_EXCEPTION = "exception"
