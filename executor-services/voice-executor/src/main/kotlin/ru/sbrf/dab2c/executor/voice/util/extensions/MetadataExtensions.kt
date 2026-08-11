package ru.sbrf.dab2c.executor.voice.util.extensions

import io.grpc.Metadata
import ru.sbrf.dab2c.executor.library.context.Headers

/** Converts gRPC metadata to domain headers, keeping only ASCII entries. */
fun Metadata.toHeaders(): Headers {
    val result = mutableMapOf<String, String>()
    for (key in keys()) {
        if (key.endsWith(Metadata.BINARY_HEADER_SUFFIX)) continue

        val asciiKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
        getAll(asciiKey)?.lastOrNull()?.let { value ->
            result[key.lowercase()] = value
        }
    }
    return Headers(result.toMap())
}
