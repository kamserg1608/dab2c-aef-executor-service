package ru.sbrf.dab2c.executor.voice.util.extensions

import io.grpc.Metadata
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

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
