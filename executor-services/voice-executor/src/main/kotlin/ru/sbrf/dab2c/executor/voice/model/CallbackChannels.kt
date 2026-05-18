package ru.sbrf.dab2c.executor.voice.model

import kotlinx.coroutines.channels.Channel
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse

/**
 * Container for callback channels used in async processing.
 */
data class CallbackChannels(
    val downstream: Channel<GigaVoiceRequest>,
    val upstream: Channel<GigaVoiceResponse>
) {
    /** Closes both channels, unblocking any suspended senders. */
    fun close() {
        downstream.close()
        upstream.close()
    }
}
