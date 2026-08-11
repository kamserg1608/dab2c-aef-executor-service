package ru.sbrf.dab2c.executor.clients.giga.agent.model

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.header

/**
 * Context containing all data needed for GigaAgent API requests.
 * Consolidates authentication headers, tracing headers, and request metadata.
 */
data class GigaAgentRequestContext(
    val ufsSession: String,
    val ufsToken: String,
    val channel: String,
    val conversationId: String,
    val eduId: String,
    val traceId: String,
    val daRequestId: String,
    val daSessionId: String,
    val daChannel: String,
    val daPlatform: String,
    val daUcpId: String?
) {

    /**
     * Applies all HTTP headers from this context to the request builder.
     */
    fun HttpRequestBuilder.applyHeaders() {
        header(UFS_SESSION, ufsSession)
        header(X_TRACE_ID_HEADER, traceId)
        header(DA_REQUEST_ID_HEADER, daRequestId)
        header(DA_SESSION_ID_HEADER, daSessionId)
        header(DA_CHANNEL_HEADER, daChannel)
        header(DA_PLATFORM_HEADER, daPlatform)
        daUcpId?.let { header(DA_UCP_ID_HEADER, it) }
    }

    /**
     * Applies cookies from this context to the request builder.
     */
    fun HttpRequestBuilder.applyCookies() {
        cookie(UFS_SESSION, ufsSession, httpOnly = true)
        cookie(UFS_TOKEN, ufsToken, httpOnly = true)
    }

    /** Returns HTTP headers as a map, matching the headers applied by [applyHeaders]. */
    fun toHeadersMap(): Map<String, String> = buildMap {
        put(UFS_SESSION, ufsSession)
        put(X_TRACE_ID_HEADER, traceId)
        put(DA_REQUEST_ID_HEADER, daRequestId)
        put(DA_SESSION_ID_HEADER, daSessionId)
        put(DA_CHANNEL_HEADER, daChannel)
        put(DA_PLATFORM_HEADER, daPlatform)
        daUcpId?.let { put(DA_UCP_ID_HEADER, it) }
    }

    /** Returns HTTP cookies as a map, matching the cookies applied by [applyCookies]. */
    fun toCookiesMap(): Map<String, String> = mapOf(
        UFS_SESSION to ufsSession,
        UFS_TOKEN to ufsToken
    )

    private companion object {
        const val UFS_SESSION = "UFS-SESSION"
        const val UFS_TOKEN = "UFS-TOKEN"
        const val X_TRACE_ID_HEADER = "X-Trace-Id"
        const val DA_REQUEST_ID_HEADER = "Da-Request-Id"
        const val DA_SESSION_ID_HEADER = "Da-Session-Id"
        const val DA_CHANNEL_HEADER = "Da-Channel"
        const val DA_PLATFORM_HEADER = "Da-Platform"
        const val DA_UCP_ID_HEADER = "Da-Ucp-Id"
    }
}
