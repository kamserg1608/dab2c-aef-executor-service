package ru.sbrf.dab2c.executor.voice

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import net.devh.boot.grpc.server.service.GrpcService

private val logger = KotlinLogging.logger {}

/**
 * Service implementation for Giga-voice proto.
 */
@GrpcService
class GigaVoiceProxyService(
    private val downstreamClient: GigaVoiceDownstreamClient
) : GigaVoiceServiceCoroutineImplBase() {

    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        logger.info { "Starting bidirectional stream proxy" }

        val loggedRequests = requests.onEach { request ->
            logger.info { "Received request: type=${request.requestCase}" }
        }

        return downstreamClient.forward(loggedRequests)
            .onEach { response ->
                logger.info { "Received response: type=${response.responseCase}" }
            }
    }
}
