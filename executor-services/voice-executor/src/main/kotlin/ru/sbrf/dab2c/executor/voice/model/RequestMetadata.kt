package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import java.util.UUID

/**
 * Wrapper for gRPC request metadata (headers) with Kotlin Map delegation.
 * Provides type-safe access to request headers captured at session start.
 * All keys are normalized to lowercase for case-insensitive access.
 */
class RequestMetadata private constructor(
    val headers: Map<String, String>,
) : Map<String, String> by headers {

    /** Internal field populated by onStart, before any processing. */
    @Suppress("VariableNaming")
    @Volatile
    internal var _daSessionInfo: DaSessionInfo? = null

    /** Public non-null accessor. Use only after onStart has completed. */
    val daSessionInfo: DaSessionInfo
        get() = _daSessionInfo ?: error("DaSessionInfo not initialized")

    /** Retrieves required header value. Throws if not present. */
    fun getHeader(header: RequestHeader): String =
        headers[header.headerName] ?: error("${header.headerName} header is required")

    /** Retrieves optional header value. Returns null if not present. */
    fun getHeaderOrNull(header: RequestHeader): String? = headers[header.headerName]

    /** Companion object providing factory methods and constants. */
    companion object {
        /** Empty metadata instance for default values. */
        val EMPTY = invoke(emptyMap())

        /** Creates RequestMetadata, generating X-Request-Id if not present. */
        operator fun invoke(headers: Map<String, String>): RequestMetadata {
            val headersWithRequestId = if (headers.containsKey(RequestHeader.X_REQUEST_ID.headerName)) {
                headers
            } else {
                headers + (RequestHeader.X_REQUEST_ID.headerName to UUID.randomUUID().toString())
            }
            return RequestMetadata(headersWithRequestId)
        }
    }
}
