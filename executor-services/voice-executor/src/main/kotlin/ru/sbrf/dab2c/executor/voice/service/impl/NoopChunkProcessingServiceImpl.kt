package ru.sbrf.dab2c.executor.voice.service.impl

import GigaVoiceProtocol.GigaVoice
import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

class NoopChunkProcessingServiceImpl: ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<GigaVoice.GigaVoiceRequest>
    ): Flow<GigaVoice.GigaVoiceRequest> = requestsChunks

    override fun processResponseChunks(
        responsesChunks: Flow<GigaVoice.GigaVoiceResponse>
    ): Flow<GigaVoice.GigaVoiceResponse> = responsesChunks
}