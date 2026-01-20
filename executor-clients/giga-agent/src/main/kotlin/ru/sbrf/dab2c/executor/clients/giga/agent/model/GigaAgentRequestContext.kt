package ru.sbrf.dab2c.executor.clients.giga.agent.model

import io.ktor.client.request.HttpRequestBuilder
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
    val daRequestId: String,
    val daSessionId: String,
    val daChannel: String,
    val daPlatform: String,
    val daUcpId: String
) {
    /**
     * Applies all HTTP headers from this context to the request builder.
     */
    fun HttpRequestBuilder.applyHeaders() {
        header(UFS_SESSION_HEADER, ufsSession)
        header(UFS_TOKEN_HEADER, ufsToken)
        header(DA_REQUEST_ID_HEADER, daRequestId)
        header(DA_SESSION_ID_HEADER, daSessionId)
        header(DA_CHANNEL_HEADER, daChannel)
        header(DA_PLATFORM_HEADER, daPlatform)
        header(DA_UCP_ID_HEADER, daUcpId)
    }

    private companion object {
        const val UFS_SESSION_HEADER = "UFS-SESSION"
        const val UFS_TOKEN_HEADER = "UFS-TOKEN"
        const val DA_REQUEST_ID_HEADER = "da-request-id"
        const val DA_SESSION_ID_HEADER = "da-session-id"
        const val DA_CHANNEL_HEADER = "da-channel"
        const val DA_PLATFORM_HEADER = "da-platform"
        const val DA_UCP_ID_HEADER = "da-ucp-id"
    }
}
