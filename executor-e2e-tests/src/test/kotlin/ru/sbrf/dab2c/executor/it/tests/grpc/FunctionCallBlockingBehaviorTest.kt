package ru.sbrf.dab2c.executor.it.tests.grpc

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.audioResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsWithDelay
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class FunctionCallBlockingBehaviorTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `backend function call is async - audio flows immediately while function executes`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithDelay(
            "get_account_balance",
            """{"balance": 1000}""",
            FUNCTION_DELAY_MS
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("blocking-test-call"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "123"}"""))
            mock.sendResponse(audioResponse(1))
            mock.sendResponse(audioResponse(2))
            mock.sendResponse(audioResponse(3))

            val timeToReceiveAllAudio = measureTime {
                withTimeout(5.seconds) {
                    session.awaitResponse { it.hasOutput() }
                    session.awaitResponse { it.hasOutput() }
                    session.awaitResponse { it.hasOutput() }
                }
            }

            assertThat(timeToReceiveAllAudio)
                .describedAs("Audio should arrive immediately, not blocked by 2s function call")
                .isLessThan(1.seconds)

            wireMock.awaitPostCall("/functions")
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `backend function result arrives at downstream after async execution completes`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithDelay(
            "get_account_balance",
            """{"balance": 1000}""",
            FUNCTION_DELAY_MS
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("blocking-test-call"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "123"}"""))
            mock.sendResponse(audioResponse(1))

            session.awaitResponse { it.hasOutput() }

            val functionResultRequest = withTimeout(5.seconds) {
                mock.awaitRequest { it.hasFunctionResult() }
            }

            assertThat(functionResultRequest.functionResult.functionName).isEqualTo("get_account_balance")
            assertThat(functionResultRequest.functionResult.content).contains("balance")
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `IVR function is proxied to client - no backend call made`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("blocking-test-call"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse("Hello"))
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            val totalTime = measureTime {
                mock.sendResponse(functionCallingResponse("transfer_to_operator", """{"reason": "test"}"""))
                mock.sendResponse(audioResponse(1))
                mock.sendResponse(audioResponse(2))

                session.awaitResponse { it.hasFunctionCall() }
                session.awaitResponse { it.hasOutput() }
                session.awaitResponse { it.hasOutput() }
            }

            assertThat(totalTime).isLessThan(1.seconds)
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    companion object {
        private const val FUNCTION_DELAY_MS = 2000
    }
}
