package ru.sbrf.dab2c.executor.voice.grpc.client

import GigaVoiceProtocol.GigaVoice
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Channel
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component

/**
 * Client for Giga-voice.
 */
@Component
class GigaVoiceDownstreamClient {

    private val logger = KotlinLogging.logger {}

    @GrpcClient("downstream")
    private lateinit var channel: Channel

    private val stub: GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub by lazy {
        GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub(channel)
    }

    fun forward(requests: Flow<GigaVoice.GigaVoiceRequest>): Flow<GigaVoice.GigaVoiceResponse> {
        logger.debug { "Forwarding request stream to downstream service" }
        return stub.gigaVoice(requests)
    }
}