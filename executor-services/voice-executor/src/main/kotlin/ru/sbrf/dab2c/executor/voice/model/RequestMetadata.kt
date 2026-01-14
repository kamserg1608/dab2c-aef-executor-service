package ru.sbrf.dab2c.executor.voice.model

/**
 * Wrapper for gRPC request metadata (headers) with Kotlin Map delegation.
 * Provides type-safe access to request headers captured at session start.
 */
class RequestMetadata(
    private val headers: Map<String, String>
) : Map<String, String> by headers {

    /** Companion object providing factory methods and constants. */
    companion object {
        /** Empty metadata instance for default values. */
        val EMPTY = RequestMetadata(emptyMap())
    }
}
