package ru.sbrf.dab2c.executor.library.tracing.facade

/**
 * Wraps callable block in `output_request` span.
 */
interface AefHttpOutgoingRequestTracing {
    /**
     * Execute callable block and send `output_request` span to AEF.
     */
    suspend fun <T : Any> trace(
        spanName: String,
        path: String,
        request: Any,
        block: suspend () -> T
    ): T
}
