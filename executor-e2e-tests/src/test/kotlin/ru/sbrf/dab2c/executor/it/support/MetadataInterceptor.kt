package ru.sbrf.dab2c.executor.it.support

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor

/**
 * gRPC client interceptor that adds metadata headers to outgoing calls.
 * Used in tests to simulate request headers like proxy mode, session, token.
 */
class MetadataInterceptor(
    private val headers: Map<String, String>
) : ClientInterceptor {

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, metadata: Metadata) {
                headers.forEach { (key, value) ->
                    metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
                }
                super.start(responseListener, metadata)
            }
        }
    }
}
