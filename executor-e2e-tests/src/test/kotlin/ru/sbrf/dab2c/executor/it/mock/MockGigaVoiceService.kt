package ru.sbrf.dab2c.executor.it.mock

import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import GigaVoiceProtocol.GigaVoice.GigaVoiceResponse
import GigaVoiceProtocol.GigaVoice.OutputTranscription
import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock GigaVoice gRPC service for integration testing.
 * Records received requests and returns sequential responses.
 */
@Component
class MockGigaVoiceService : GigaVoiceServiceCoroutineImplBase() {

    private val _receivedRequests = mutableListOf<GigaVoiceRequest>()
    private val _responseCounter = AtomicInteger(0)

    /** List of all requests received by this mock. */
    val receivedRequests: List<GigaVoiceRequest>
        get() = _receivedRequests.toList()

    /** Resets the mock state (clears requests and counter). */
    fun reset() {
        _receivedRequests.clear()
        _responseCounter.set(0)
    }

    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> = requests.map { request ->
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
