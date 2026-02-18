package ru.sbrf.dab2c.executor.voice.model

import kotlinx.coroutines.channels.Channel
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * Container for callback channels used in async processing.
 */
data class CallbackChannels(
    val downstream: Channel<VoiceRequest>,
    val upstream: Channel<VoiceResponse>
) {
    /** Closes both channels, unblocking any suspended senders. */
    fun close() {
        downstream.close()
        upstream.close()
    }
}
