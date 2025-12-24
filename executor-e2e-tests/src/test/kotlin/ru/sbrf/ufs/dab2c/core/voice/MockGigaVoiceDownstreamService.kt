package ru.sbrf.ufs.dab2c.core.voice

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
class MockGigaVoiceDownstreamService : GigaVoiceServiceCoroutineImplBase() {

    private val _receivedRequests = mutableListOf<GigaVoiceRequest>()
    private val _responseCounter = AtomicInteger(0)

    val receivedRequests: List<GigaVoiceRequest>
        get() = _receivedRequests.toList()

    fun reset() {
        _receivedRequests.clear()
        _responseCounter.set(0)
    }

    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        return requests.map { request ->
            _receivedRequests.add(request)
            val index = _responseCounter.getAndIncrement()
            GigaVoiceResponse.newBuilder()
                .setOutputTranscription(
                    OutputTranscription.newBuilder()
                        .setText("response-$index")
                        .build()
                )
                .build()
        }
    }
}
