package ru.sbrf.dab2c.executor.voice.service.api

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * Service for processing voice request and response chunks.
 */
interface ChunkProcessingService {

    /** Processes request chunks before sending to GigaVoice. */
    fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest>

    /** Processes response chunks from GigaVoice. */
    fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse>
}
