package ru.sbrf.dab2c.executor.voice.service.api

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

interface ChunkProcessingService {

    fun processRequestChunks(requestsChunks: Flow<VoiceRequest>): Flow<VoiceRequest>

    fun processResponseChunks(responsesChunks: Flow<VoiceResponse>): Flow<VoiceResponse>
}
