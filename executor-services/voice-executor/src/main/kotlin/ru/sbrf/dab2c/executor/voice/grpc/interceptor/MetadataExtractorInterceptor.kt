package ru.sbrf.dab2c.executor.voice.grpc.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.util.extensions.toRequestMetadata

/**
 * gRPC server interceptor that extracts all request metadata (headers)
 * and stores them in the gRPC Context for downstream access.
 */
@GrpcGlobalServerInterceptor
class MetadataExtractorInterceptor : ServerInterceptor {

    private val logger = KotlinLogging.logger {}

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val requestMetadata = headers.toRequestMetadata()

        logger.debug {
            "Extracted ${requestMetadata.size} metadata entries for ${call.methodDescriptor.fullMethodName}"
        }
        logger.trace { "Metadata: $requestMetadata" }

        val contextWithMetadata = Context.current()
            .withValue(GrpcMetadataContext.METADATA_KEY, requestMetadata)

        return Contexts.interceptCall(contextWithMetadata, call, headers, next)
    }
}
