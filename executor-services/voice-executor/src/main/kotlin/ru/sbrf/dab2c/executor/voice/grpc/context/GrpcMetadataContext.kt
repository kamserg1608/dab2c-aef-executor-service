package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Context
import io.grpc.Metadata
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * gRPC context key for storing request metadata.
 * Used to pass metadata from interceptor to service methods.
 */
object GrpcMetadataContext {

    /**
     * Context key for storing extracted metadata.
     */
    val METADATA_KEY: Context.Key<RequestMetadata> =
        Context.key("grpc-request-metadata")

    /**
     * Retrieves the current request metadata from coroutine context,
     * falling back to gRPC context if not present.
     * This is the main API for accessing metadata in suspend functions.
     */
    suspend fun current(): RequestMetadata =
        coroutineContext[MetadataElement]?.metadata ?: fromGrpcThread()

    /**
     * Creates a coroutine context element with the current metadata.
     * Checks coroutine context first, then falls back to gRPC context.
     * Use when launching new coroutines that need metadata access.
     */
    suspend fun asCoroutineContext(): CoroutineContext =
        coroutineContext[MetadataElement] ?: MetadataElement(fromGrpcThread())

    /**
     * Captures metadata from gRPC thread-local context into a coroutine context element.
     * Use at flow entry points to propagate metadata into flow processing.
     * Must be called on the gRPC thread where metadata is available.
     */
    fun captureGrpcContext(): CoroutineContext =
        MetadataElement(fromGrpcThread())

    /**
     * Reads metadata directly from gRPC thread-local context.
     * Use only in non-suspend contexts (e.g., factory methods) that run on the gRPC thread.
     * For suspend functions, use [current] instead.
     */
    fun fromGrpcThread(): RequestMetadata =
        METADATA_KEY.get() ?: RequestMetadata.EMPTY

    /**
     * Converts gRPC [Metadata] to [RequestMetadata].
     * Keys are normalized to lowercase for case-insensitive access.
     * For keys with multiple values, the last value is used.
     * Binary keys (ending with "-bin") are skipped.
     */
    fun Metadata.toRequestMetadata(): RequestMetadata {
        val result = mutableMapOf<String, String>()
        for (key in keys()) {
            if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue

            val asciiKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
            getAll(asciiKey)?.lastOrNull()?.let { value ->
                result[key.lowercase()] = value
            }
        }
        return RequestMetadata(result.toMap())
    }
}

/**
 * Coroutine context element for propagating request metadata across coroutines.
 */
class MetadataElement(val metadata: RequestMetadata) : CoroutineContext.Element {
    override val key get() = Key

    /**
     * Coroutine context key for [MetadataElement].
     */
    companion object Key : CoroutineContext.Key<MetadataElement>
}
