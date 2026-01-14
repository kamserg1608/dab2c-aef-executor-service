package ru.sbrf.dab2c.executor.voice.model

/**
 * Wrapper for gRPC request metadata (headers) with Kotlin Map delegation.
 * Provides type-safe access to request headers captured at session start.
 * All keys are normalized to lowercase for case-insensitive access.
 */
class RequestMetadata(
    private val headers: Map<String, String>
) : Map<String, String> by headers {

    /** The session header value. Throws if not present. */
    val session: String
        get() = headers[HEADER_SESSION]
            ?: error("Session header is required")

    /** The token header value. Throws if not present. */
    val token: String
        get() = headers[HEADER_TOKEN]
            ?: error("Token header is required")

    /** The edu_id header value. */
    val eduId: String? get() = headers[HEADER_EDU_ID]

    /** UFS cookie string built from session and token values. Throws if headers missing. */
    val ufsCookie: String
        get() = "$UFS_TOKEN_COOKIE=$token;$UFS_SESSION_COOKIE=$session"

    /** Companion object providing factory methods and constants. */
    companion object {
        /** Empty metadata instance for default values. */
        val EMPTY = RequestMetadata(emptyMap())

        private const val HEADER_SESSION = "session"
        private const val HEADER_TOKEN = "token"
        private const val HEADER_EDU_ID = "edu_id"

        private const val UFS_TOKEN_COOKIE = "UFS-TOKEN"
        private const val UFS_SESSION_COOKIE = "UFS-SESSION"
    }
}
