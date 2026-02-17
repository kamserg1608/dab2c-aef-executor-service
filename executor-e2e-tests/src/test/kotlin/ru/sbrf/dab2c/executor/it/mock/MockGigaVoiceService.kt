package ru.sbrf.dab2c.executor.it.mock

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineImplBase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Mock GigaVoice gRPC service for integration testing.
 * Uses controlled mode where tests explicitly send responses via sendResponse().
 */
@Component
class MockGigaVoiceService : GigaVoiceServiceCoroutineImplBase() {

    val receivedRequests = mutableListOf<GigaVoiceRequest>()
    private var requestChannel = Channel<GigaVoiceRequest>(Channel.UNLIMITED)
    private var responseChannel = Channel<GigaVoiceResponse>(Channel.UNLIMITED)
    private var collectorJob: Job? = null

    /**
     * Resets the mock state.
     */
    fun reset() {
        receivedRequests.clear()
        collectorJob?.cancel()
        collectorJob = null
        requestChannel = Channel(Channel.UNLIMITED)
        responseChannel = Channel(Channel.UNLIMITED)
    }

    override fun gigaVoice(requests: Flow<GigaVoiceRequest>): Flow<GigaVoiceResponse> {
        collectorJob = CoroutineScope(Dispatchers.IO).launch {
            requests.collect { request ->
                receivedRequests.add(request)
                requestChannel.send(request)
            }
            requestChannel.close()
            responseChannel.close()
        }

        return responseChannel.consumeAsFlow()
    }

    /**
     * Waits for a request that matches the predicate.
     */
    @Suppress("detekt:LabeledExpression")
    suspend fun awaitRequest(
        timeout: Duration = 5.seconds,
        predicate: (GigaVoiceRequest) -> Boolean = { true }
    ): GigaVoiceRequest {
        return withTimeout(timeout) {
            for (request in requestChannel) {
                if (predicate(request)) return@withTimeout request
            }
            error("Channel closed without matching request")
        }
    }

    /**
     * Sends a response to the client.
     */
    suspend fun sendResponse(response: GigaVoiceResponse) {
        responseChannel.send(response)
    }

    /**
     * Completes the response stream.
     */
    fun completeResponses() {
        responseChannel.close()
    }

    /**
     * Completes the response stream with an error.
     */
    fun completeResponsesWithError(cause: Throwable) {
        responseChannel.close(cause)
    }
}
