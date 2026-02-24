package ru.sbrf.dab2c.executor.voice.grpc.interceptor

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext
import ru.sbrf.dab2c.executor.voice.util.extensions.toHeaders

/** Server interceptor that extracts gRPC metadata into the request context. */
@GrpcGlobalServerInterceptor
class MetadataExtractorInterceptor : ServerInterceptor {

    private val logger = KotlinLogging.logger {}

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val requestHeaders = headers.toHeaders()

        logger.debug {
            "Extracted ${requestHeaders.size} metadata entries for ${call.methodDescriptor.fullMethodName}"
        }

        logger.trace { "Metadata: ${requestHeaders.asString()}" }

        val contextWithMetadata = Context.current()
            .withValue(GrpcMetadataContext.METADATA_KEY, requestHeaders)

        return Contexts.interceptCall(contextWithMetadata, call, headers, next)
    }
}

private fun Headers.asString() = this.headers.entries.joinToString("; ") { "${it.key}=${it.value}" }
