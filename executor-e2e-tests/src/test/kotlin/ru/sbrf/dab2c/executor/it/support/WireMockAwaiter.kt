package ru.sbrf.dab2c.executor.it.support

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.verification.LoggedRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Utility for waiting on HTTP calls to WireMock endpoints.
 * Replaces arbitrary delays with explicit synchronization.
 */
@Suppress("detekt:LabeledExpression")
class WireMockAwaiter(private val server: WireMockServer) {

    /**
     * Waits until at least one POST request has been made to the specified URL.
     */
    suspend fun awaitPostCall(
        url: String,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 50.milliseconds
    ): LoggedRequest = withTimeout(timeout) {
        while (true) {
            val requests = server.findAll(postRequestedFor(urlEqualTo(url)))
            if (requests.isNotEmpty()) {
                return@withTimeout requests.first()
            }
            delay(pollInterval)
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }

    /**
     * Waits until at least one POST request has been made matching the URL pattern.
     */
    suspend fun awaitPostCallMatching(
        urlPattern: String,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 50.milliseconds
    ): LoggedRequest = withTimeout(timeout) {
        while (true) {
            val requests = server.findAll(postRequestedFor(urlMatching(urlPattern)))
            if (requests.isNotEmpty()) {
                return@withTimeout requests.first()
            }
            delay(pollInterval)
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }

    /**
     * Waits until the specified number of POST requests have been made to the URL.
     */
    suspend fun awaitPostCalls(
        url: String,
        count: Int,
        timeout: Duration = 5.seconds,
        pollInterval: Duration = 50.milliseconds
    ): List<LoggedRequest> = withTimeout(timeout) {
        while (true) {
            val requests = server.findAll(postRequestedFor(urlEqualTo(url)))
            if (requests.size >= count) {
                return@withTimeout requests.take(count)
            }
            delay(pollInterval)
        }
        @Suppress("UNREACHABLE_CODE")
        error("Unreachable")
    }
}
