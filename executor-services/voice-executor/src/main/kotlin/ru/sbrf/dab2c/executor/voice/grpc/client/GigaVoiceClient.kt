package ru.sbrf.dab2c.executor.voice.grpc.client

import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt
import ru.sbrf.dab2c.executor.voice.logging.CompletionOutcome
import ru.sbrf.dab2c.executor.voice.logging.completionOutcome

/** gRPC client for bidirectional streaming communication with the downstream GigaVoice service. */
@Component
class GigaVoiceClient {

    private val logger = KotlinLogging.logger {}

    @GrpcClient("downstream")
    private lateinit var channel: Channel

    private val stub: GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub by lazy {
        GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub(channel)
    }

    /** Opens a bidirectional streaming session with the downstream GigaVoice service. */
    fun session(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        logger.debug { "Starting bidirectional session with GigaVoice" }
        return stub.gigaVoice(requests)
            .onCompletion { cause ->
                when {
                    cause == null -> Unit
                    completionOutcome(cause) == CompletionOutcome.CANCELLED ->
                        logger.debug { "GigaVoice downstream session cancelled: target=${channel.authority()}" }
                    else -> logger.warn {
                        "GigaVoice downstream session failed: target=${channel.authority()}, " +
                            "error=${cause::class.simpleName}: ${cause.message}"
                    }
                }
            }
    }
}
