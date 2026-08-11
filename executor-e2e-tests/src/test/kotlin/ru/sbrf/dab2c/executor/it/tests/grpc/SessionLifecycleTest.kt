package ru.sbrf.dab2c.executor.it.tests.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.coroutineScope
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.TestSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubEfsRestAgent
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsWithDelay
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithDelay
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Verifies that gRPC stream lifecycle cleanup works correctly
 * when either the IVR client or the downstream GigaVoice service disconnects.
 */
class SessionLifecycleTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `downstream stream closes when IVR disconnects during active session`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        coroutineScope {
            val session = TestSession(testStub())
            session.start(this)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("lifecycle-active-disconnect"))
            mockGigaVoiceService.awaitRequest { it.hasSettings() }

            mockGigaVoiceService.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mockGigaVoiceService.awaitRequest { it.hasInput() }

            session.closeRequests()
            mockGigaVoiceService.awaitStreamClosed()

            session.cancel()
        }
    }

    @Test
    fun `downstream stream closes when IVR disconnects during settings calculation`() = runItTest {
        efsAdapterMock.stubEfsRestAgent()
        gigaVoiceAgentMock.stubGigaAgentSettingsWithDelay(ASYNC_DELAY_MS)

        coroutineScope {
            val session = TestSession(testStub())
            session.start(this)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("lifecycle-settings-disconnect"))

            val wireMock = WireMockAwaiter(gigaVoiceAgentMock)
            wireMock.awaitPostCall("/settings")

            session.closeRequests()
            mockGigaVoiceService.awaitStreamClosed()

            session.cancel()
        }
    }

    @Test
    fun `downstream stream closes when IVR disconnects during function execution`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithDelay(
            "get_account_balance", """{"balance": 1000}""", ASYNC_DELAY_MS
        )

        coroutineScope {
            val session = TestSession(testStub())
            session.start(this)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("lifecycle-function-disconnect"))
            mockGigaVoiceService.awaitRequest { it.hasSettings() }

            mockGigaVoiceService.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mockGigaVoiceService.awaitRequest { it.hasInput() }

            mockGigaVoiceService.sendResponse(
                functionCallingResponse("get_account_balance", """{"account_id": "123"}""")
            )

            val wireMock = WireMockAwaiter(gigaVoiceAgentMock)
            wireMock.awaitPostCall("/functions")

            session.closeRequests()
            mockGigaVoiceService.awaitStreamClosed()

            session.cancel()
        }
    }

    @Test
    fun `session ends when GigaVoice disconnects with error`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        coroutineScope {
            val session = TestSession(testStub())
            session.start(this)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("lifecycle-gigavoice-error"))
            mockGigaVoiceService.awaitRequest { it.hasSettings() }

            mockGigaVoiceService.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            mockGigaVoiceService.completeResponsesWithError(
                StatusRuntimeException(Status.INTERNAL.withDescription("server error"))
            )

            session.awaitCompletion()
            session.cancel()
        }
    }

    @Test
    fun `session ends when GigaVoice closes normally`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        coroutineScope {
            val session = TestSession(testStub())
            session.start(this)

            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("lifecycle-gigavoice-close"))
            mockGigaVoiceService.awaitRequest { it.hasSettings() }

            mockGigaVoiceService.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            mockGigaVoiceService.completeResponses()

            session.awaitCompletion()
            session.cancel()
        }
    }

    companion object {
        private const val ASYNC_DELAY_MS = 3000
    }
}
