package ru.sbrf.dab2c.executor.it.support.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Controlled test session for IVR client interactions.
 * Provides explicit send/receive operations instead of relying on flow semantics.
 */
class TestSession(
    private val stub: GigaVoiceServiceCoroutineStub
) {
    private val requestChannel = Channel<GigaVoiceRequest>(Channel.UNLIMITED)
    private val responseChannel = Channel<GigaVoiceResponse>(Channel.UNLIMITED)
    val receivedResponses = mutableListOf<GigaVoiceResponse>()
    private var collectJob: Job? = null

    /**
     * Starts the gRPC session in the given coroutine scope.
     * Must be called before sendRequest/awaitResponse.
     */
    fun start(scope: CoroutineScope) {
        collectJob = scope.launch {
            @Suppress("SwallowedException", "TooGenericExceptionCaught")
            try {
                stub.gigaVoice(requestChannel.consumeAsFlow()).collect { response ->
                    receivedResponses.add(response)
                    responseChannel.send(response)
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Sends a request to the server.
     */
    suspend fun sendRequest(request: GigaVoiceRequest) {
        requestChannel.send(request)
    }

    /**
     * Waits for the next response that matches the predicate.
     */
    @Suppress("detekt:LabeledExpression")
    suspend fun awaitResponse(
        timeout: Duration = 5.seconds,
        predicate: (GigaVoiceResponse) -> Boolean = { true }
    ): GigaVoiceResponse = withTimeout(timeout) {
        for (response in responseChannel) {
            if (predicate(response)) return@withTimeout response
        }
        error("Channel closed without matching response")
    }

    /**
     * Closes the request stream (signals no more requests).
     */
    fun closeRequests() {
        requestChannel.close()
    }

    /**
     * Waits for the response collection to finish naturally, failing if it is still running after
     * [timeout] — the server never closed the response stream.
     */
    suspend fun awaitCompletionOrFail(timeout: Duration = 2.seconds) {
        withTimeout(timeout) {
            collectJob?.join()
        }
    }

    /**
     * Waits for the response collection to finish naturally before forced cancellation.
     */
    suspend fun awaitCompletion(timeout: Duration = 2.seconds) {
        @Suppress("SwallowedException")
        try {
            awaitCompletionOrFail(timeout)
        } catch (_: TimeoutCancellationException) {
        }
    }

    /**
     * Cancels the session collection job.
     */
    fun cancel() {
        collectJob?.cancel()
    }
}
