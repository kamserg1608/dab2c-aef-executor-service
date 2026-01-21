package ru.sbrf.dab2c.executor.it.support.session

import com.github.tomakehurst.wiremock.WireMockServer
import kotlinx.coroutines.coroutineScope
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt.IvrServiceCoroutineStub
import ru.sbrf.dab2c.executor.it.mock.MockGigaVoiceService
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter

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
 * Runs a test session with explicit synchronization.
 */
suspend fun withSession(
    stub: IvrServiceCoroutineStub,
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
        session.cancel()
        mock.completeResponses()
    }
}
