package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/**
 * No-op implementation of ChunkProcessingService (pass-through mode).
 */
class NoopChunkProcessingServiceImpl : ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> = requestsChunks

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> = responsesChunks
}
