package ru.sbrf.dab2c.executor.voice.service.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

class ObservingChunkProcessingServiceDelegate(
    private val delegate: ChunkProcessingService
) : ChunkProcessingService {

    private val logger = KotlinLogging.logger { }

    override fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest> {
        logger.info { "Starting bidirectional request stream proxy" }
        val observedChunks = requestsChunks
            .onEach {
                logger.info { "Received request: type=${it::class.simpleName}" }
                logger.debug { "Received request: type=${it::class.simpleName}, request=$it" }
            }
        return delegate.processRequestChunks(observedChunks)
    }

    override fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse> {
        logger.info { "Starting bidirectional response stream proxy" }
        val observedChunks = responsesChunks
            .onEach {
                logger.info { "Received response: type=${it::class.simpleName}" }
                logger.debug { "Received response: type=${it::class.simpleName}, response=$it" }
            }
        return delegate.processResponseChunks(observedChunks)
    }
}
