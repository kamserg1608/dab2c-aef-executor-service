package ru.sbrf.dab2c.executor.voice.service.api

import GigaVoiceProtocol.GigaVoice
import kotlinx.coroutines.flow.Flow

interface ChunkProcessingService {

    fun processRequestChunks(requestsChunks: Flow<GigaVoice.GigaVoiceRequest>): Flow<GigaVoice.GigaVoiceRequest>

    fun processResponseChunks(responsesChunks: Flow<GigaVoice.GigaVoiceResponse>): Flow<GigaVoice.GigaVoiceResponse>

}