package ru.sbrf.dab2c.executor.clients.gigavoice

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * GigaVoice client interface exposing domain types.
 * Hides gRPC/proto details from consumers.
 */
interface GigaVoiceClient {

    /**
     * Starts a bidirectional voice session.
     *
     * @param requests Flow of requests (settings first, then audio/function results)
     * @return Flow of responses (audio, transcriptions, function calls, etc.)
     */
    fun session(requests: Flow<VoiceRequest>): Flow<VoiceResponse>
}
