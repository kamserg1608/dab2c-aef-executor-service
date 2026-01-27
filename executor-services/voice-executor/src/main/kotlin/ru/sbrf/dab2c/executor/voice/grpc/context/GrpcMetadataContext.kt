package ru.sbrf.dab2c.executor.voice.grpc.context

import io.grpc.Context
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
     * Reads metadata directly from gRPC thread-local context.
     * Use only in non-suspend contexts that run on the gRPC thread.
     */
    fun fromGrpcThread(): RequestMetadata =
        METADATA_KEY.get() ?: RequestMetadata.EMPTY
}
