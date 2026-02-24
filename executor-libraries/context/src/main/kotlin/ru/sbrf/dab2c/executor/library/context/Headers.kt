package ru.sbrf.dab2c.executor.library.context

import java.util.UUID

/**
 * Immutable wrapper around request headers with typed access via [RequestHeader].
 */
class Headers private constructor(
    val headers: Map<String, String>,
) : Map<String, String> by headers {

    /** Returns the header value or throws if absent. */
    fun getHeader(header: RequestHeader): String =
        headers[header.headerName] ?: error("${header.headerName} header is required")

    /** Returns the header value or null if absent. */
    fun getHeaderOrNull(header: RequestHeader): String? = headers[header.headerName]

    /** Factory and constants. */
    companion object {
        val EMPTY = invoke(emptyMap())

        /** Creates [Headers], auto-generating x-request-id when absent. */
        operator fun invoke(headers: Map<String, String>): Headers {
            val headersWithRequestId = if (headers.containsKey(RequestHeader.X_REQUEST_ID.headerName)) {
                headers
            } else {
                headers + (RequestHeader.X_REQUEST_ID.headerName to UUID.randomUUID().toString())
            }
            return Headers(headersWithRequestId)
        }
    }
}

val Headers.ufsCookie: String
    get() = "UFS-TOKEN=${getHeader(RequestHeader.TOKEN)};UFS-SESSION=${getHeader(RequestHeader.SESSION)}"
