package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Context
import io.grpc.Metadata
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

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
     * Retrieves the current request metadata from gRPC context.
     * Returns [RequestMetadata.EMPTY] if no metadata is present.
     */
    fun current(): RequestMetadata =
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
