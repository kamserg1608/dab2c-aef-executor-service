package ru.sbrf.dab2c.executor.voice.grpc.client

import GigaVoiceProtocol.GigaVoice
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Channel
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component

/**
 * gRPC client for GigaVoice service.
 */
@Component
class GigaVoiceClient {

    private val logger = KotlinLogging.logger {}

    @GrpcClient("downstream")
    private lateinit var channel: Channel

    private val stub: GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub by lazy {
        GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub(channel)
    }

    fun session(requests: Flow<GigaVoice.GigaVoiceRequest>): Flow<GigaVoice.GigaVoiceResponse> {
        logger.debug { "Sending request stream to GigaVoice service" }
        return stub.gigaVoice(requests)
    }
}
