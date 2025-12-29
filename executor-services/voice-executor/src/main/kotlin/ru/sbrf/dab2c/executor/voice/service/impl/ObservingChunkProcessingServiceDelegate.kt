package ru.sbrf.dab2c.executor.voice.service.impl

import GigaVoiceProtocol.GigaVoice
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

class ObservingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
): ChunkProcessingService {

    private val logger = KotlinLogging.logger { }

    override fun processRequestChunks(requestsChunks: Flow<GigaVoice.GigaVoiceRequest>): Flow<GigaVoice.GigaVoiceRequest> {
        logger.info { "Starting bidirectional request stream proxy" }
        val observedChunks = requestsChunks
            .onEach {
                logger.info { "Received request: type=${it.requestCase}" }
                logger.debug { logger.info { "Received request: type=${it.requestCase}, request=${it}" } }
            }
        return delegate.processRequestChunks(observedChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<GigaVoice.GigaVoiceResponse>): Flow<GigaVoice.GigaVoiceResponse> {
        logger.info { "Starting bidirectional response stream proxy" }
        val observedChunks = responsesChunks
            .onEach {
                logger.info { "Received response: type=${it.responseCase}" }
                logger.debug { logger.info { "Received response: type=${it.responseCase}, request=${it}" } }
            }
        return delegate.processResponseChunks(observedChunks)
    }
}