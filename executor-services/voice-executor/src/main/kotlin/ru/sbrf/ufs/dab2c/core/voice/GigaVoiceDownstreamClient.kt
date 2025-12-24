package ru.sbrf.ufs.dab2c.core.voice

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.Channel
import kotlinx.coroutines.flow.Flow
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class GigaVoiceDownstreamClient {

    @GrpcClient("downstream")
    private lateinit var channel: Channel

    private val stub: GigaVoiceServiceCoroutineStub by lazy {
        GigaVoiceServiceCoroutineStub(channel)
    }

    fun forward(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        logger.debug { "Forwarding request stream to downstream service" }
        return stub.gigaVoice(requests)
    }
}
