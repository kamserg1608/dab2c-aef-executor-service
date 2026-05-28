package ru.sbrf.dab2c.executor.library.tracing.grpc

import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.opentelemetry.context.Context
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order

/**
 * Captures the OTel Context established by the AEF SDK voice interceptor on the gRPC thread
 * and stashes it in the gRPC Context so coroutine-side code can pick it up.
 *
 * Registered with the lowest precedence so it runs after the SDK's `VoiceGrpcServerInterceptor`.
 */
@GrpcGlobalServerInterceptor
@Order(Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(name = ["aef.tracing.voice-enabled"], havingValue = "true", matchIfMissing = true)
class OtelContextCapturingInterceptor : ServerInterceptor {

    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val capturedOtel = Context.current()
        val grpcCtx = io.grpc.Context.current().withValue(OtelGrpcBridge.OTEL_CTX_KEY, capturedOtel)
        return Contexts.interceptCall(grpcCtx, call, headers, next)
    }
}
