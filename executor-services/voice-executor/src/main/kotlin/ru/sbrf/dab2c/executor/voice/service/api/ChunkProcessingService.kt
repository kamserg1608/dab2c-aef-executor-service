package ru.sbrf.dab2c.executor.voice.service.api

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse

/**
 * Service for processing voice request and response chunks.
 */
interface ChunkProcessingService {

    /** Processes request chunks before sending to GigaVoice. */
    fun processRequestChunks(requestsChunks: Flow<GigaVoiceRequest>): Flow<GigaVoiceRequest>

    /** Processes response chunks from GigaVoice. */
    fun processResponseChunks(responsesChunks: Flow<GigaVoiceResponse>): Flow<GigaVoiceResponse>
}
