package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Context
import ru.sbrf.dab2c.executor.library.context.Headers

/** Holds gRPC request metadata in a thread-local gRPC context. */
object GrpcMetadataContext {

    val METADATA_KEY: Context.Key<Headers> =
        Context.key("grpc-request-metadata")

    /** Retrieves request headers from the current gRPC context. */
    fun fromGrpcThread(): Headers =
        METADATA_KEY.get() ?: Headers.EMPTY
}
