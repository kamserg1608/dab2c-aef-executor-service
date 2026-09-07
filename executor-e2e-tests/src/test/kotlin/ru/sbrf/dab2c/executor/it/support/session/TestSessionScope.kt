package ru.sbrf.dab2c.executor.it.support.session

import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.coroutineScope
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import ru.sbrf.dab2c.executor.it.mock.MockGigaVoiceService
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * DSL scope for controlled test sessions.
 * Provides access to session, mock, and WireMock utilities.
 */
class TestSessionScope(
    val session: TestSession,
    val mock: MockGigaVoiceService,
    val wireMock: WireMockAwaiter
)

/**
 * Half-closes the session and requires the server to close the response stream within [timeout].
 */
suspend fun TestSessionScope.closeAndAwaitCompletion(timeout: Duration = 2.seconds) {
    session.closeRequests()
    mock.completeResponses()
    session.awaitCompletionOrFail(timeout)
}

/**
 * Runs a test session with explicit synchronization.
 */
suspend fun withSession(
    stub: GigaVoiceServiceCoroutineStub,
    mock: MockGigaVoiceService,
    wireMockServer: WireMockServer,
    block: suspend TestSessionScope.() -> Unit
) = coroutineScope {
    val session = TestSession(stub)
    val awaiter = WireMockAwaiter(wireMockServer)
    val scope = TestSessionScope(session, mock, awaiter)

    session.start(this)
    try {
        scope.block()
    } finally {
        session.closeRequests()
        mock.completeResponses()
        session.awaitCompletion()
        session.cancel()
    }
}

/**
 * Runs a test session that ends by aborting a live stream: the collector coroutine is cancelled
 * before half-close, unlike [withSession] which drains the session first.
 *
 * Teardown does not wait for the downstream mock: on cancellation its request flow never completes,
 * so there is nothing to synchronise on — the caller awaits the effect it expects instead.
 */
suspend fun withAbortedSession(
    stub: GigaVoiceServiceCoroutineStub,
    mock: MockGigaVoiceService,
    wireMockServer: WireMockServer,
    block: suspend TestSessionScope.() -> Unit
) = coroutineScope {
    val session = TestSession(stub)
    val awaiter = WireMockAwaiter(wireMockServer)
    val scope = TestSessionScope(session, mock, awaiter)

    session.start(this)
    try {
        scope.block()
    } finally {
        session.cancel()
    }
}
