package ru.sbrf.dab2c.executor.library.tracing

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.Tracer
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.aef.voice.grpc.VoiceGrpcServerInterceptor
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracing
import ru.sbrf.dab2c.executor.library.tracing.facade.AefHttpOutgoingRequestTracingImpl
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacade
import ru.sbrf.dab2c.executor.library.tracing.facade.AefTracingFacadeImpl
import ru.sbrf.dab2c.executor.library.tracing.facade.NoOpAefTracingFacade

/** Wires AEF tracing beans and registers the SDK voice gRPC interceptor. */
@Configuration
class TracingConfiguration {

    /** Real facade backed by AEF SDK; active when `aef.tracing.voice-enabled` is `true` (default). */
    @Bean
    @ConditionalOnProperty(name = ["aef.tracing.voice-enabled"], havingValue = "true", matchIfMissing = true)
    fun aefTracingFacade(tracer: Tracer): AefTracingFacade = AefTracingFacadeImpl(tracer)

    /** No-op fallback so consumers can inject the facade unconditionally. */
    @Bean
    @ConditionalOnMissingBean(AefTracingFacade::class)
    fun noOpAefTracingFacade(): AefTracingFacade = NoOpAefTracingFacade()

    /**
     * Registers the AEF SDK `VoiceGrpcServerInterceptor` as a net.devh global server interceptor.
     */
    @Bean
    @ConditionalOnProperty(name = ["aef.tracing.voice-enabled"], havingValue = "true", matchIfMissing = true)
    fun aefVoiceInterceptorConfigurer(
        sdkInterceptorProvider: ObjectProvider<VoiceGrpcServerInterceptor>
    ): GlobalServerInterceptorConfigurer = GlobalServerInterceptorConfigurer { registry ->
        sdkInterceptorProvider.ifAvailable?.let(registry::add)
    }

    /**
     * AEF `outgoing_request` wrapper.
     */
    @Bean
    fun aefHttpOutgoingRequestTracing(facade: AefTracingFacade, mapper: ObjectMapper): AefHttpOutgoingRequestTracing =
        AefHttpOutgoingRequestTracingImpl(facade, mapper)
}
