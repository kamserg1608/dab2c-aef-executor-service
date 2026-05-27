package ru.sbrf.dab2c.executor.it.tests.grpc

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.platformFunctionProcessing
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockAwaiter
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithFunctionMatch
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithFunctionMatchIag
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Full Mode - Function Call Routing Tests.
 * Verifies routing of FunctionCalling between IVR and backend execution.
 */
class FunctionCallRoutingTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should proxy IVR function to client when configuratorEnabled is true`() = runItTest {
        setupStubsWithFunctionMatch(
            efsAdapterMock,
            configuratorMock,
            gigaVoiceAgentMock,
            configuratorEnabled = true
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("ivr-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                functionCallingResponse(
                    "transfer_to_operator",
                    """{"reason": "customer request"}"""
                )
            )

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }

            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("transfer_to_operator")
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should proxy IVR function to client when configuratorEnabled is false`() = runItTest {
        setupStubsWithFunctionMatch(
            efsAdapterMock,
            configuratorMock,
            gigaVoiceAgentMock,
            configuratorEnabled = false
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("ivr-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                functionCallingResponse(
                    "transfer_to_operator",
                    """{"reason": "customer request"}"""
                )
            )

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }

            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("transfer_to_operator")
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should execute backend function via GigaAgent when configuratorEnabled is true`() =
        runItTest {
            setupStubsWithFunctionMatch(
                efsAdapterMock,
                configuratorMock,
                gigaVoiceAgentMock,
                configuratorEnabled = true
            )

            gigaVoiceAgentMock.stubGigaAgentFunctions(
                "get_account_balance",
                """{"balance": 1000}"""
            )

            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse(
                        "get_account_balance",
                        """{"account_id": "12345"}"""
                    )
                )

                mock.sendResponse(platformFunctionProcessing())
                session.awaitResponse { it.hasPlatformFunctionProcessing() }

                wireMock.awaitPostCall("/functions")

                assertThat(session.receivedResponses.none { it.hasFunctionCall() }).isTrue()
            }

            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
        }

    @Test
    fun `should execute backend function via GigaAgent when configuratorEnabled is false`() =
        runItTest {
            setupStubsWithFunctionMatch(
                efsAdapterMock,
                configuratorMock,
                gigaVoiceAgentMock,
                configuratorEnabled = false
            )

            gigaVoiceAgentMock.stubGigaAgentFunctions(
                "get_account_balance",
                """{"balance": 1000}"""
            )

            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse(
                        "get_account_balance",
                        """{"account_id": "12345"}"""
                    )
                )

                mock.sendResponse(platformFunctionProcessing())
                session.awaitResponse { it.hasPlatformFunctionProcessing() }

                wireMock.awaitPostCall("/functions")

                assertThat(session.receivedResponses.none { it.hasFunctionCall() }).isTrue()
            }

            gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
        }

    @Test
    fun `should execute backend function via iag when configuratorEnabled is true`() =
        runItTest {
            setupStubsWithFunctionMatchIag(
                efsAdapterMock,
                configuratorMock,
                gigaVoiceAgentMock,
                iagMock,
                configuratorEnabled = true
            )

            withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                session.sendRequest(contextRequest())
                session.sendRequest(settingsRequest("backend-function-iag-test"))
                mock.awaitRequest { it.hasSettings() }

                mock.sendResponse(outputTranscriptionResponse())
                session.awaitResponse()

                session.sendRequest(audioRequest(speechStart = true))
                mock.awaitRequest { it.hasInput() }

                mock.sendResponse(
                    functionCallingResponse(
                        "find_bank_office_iag",
                        """{"account_id": "12345"}"""
                    )
                )

                mock.sendResponse(platformFunctionProcessing())
                session.awaitResponse { it.hasPlatformFunctionProcessing() }
                val wireMock = WireMockAwaiter(iagMock)
                wireMock.awaitPostCall("/bh")

                assertThat(session.receivedResponses.none { it.hasFunctionCall() }).isTrue()
            }

            gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
            iagMock.verify(1, postRequestedFor(urlEqualTo("/bh")))
        }

    @Test
    fun `should proxy IVR function to client when isBackendFunction is false`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("ivr-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(
                functionCallingResponse("transfer_to_operator", """{"reason": "customer request"}""")
            )

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }
            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("transfer_to_operator")
        }
        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should proxy unknown function to IVR client by default`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("unknown-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("unknown_function_not_in_registry", """{"data": "test"}"""))

            val functionResponse = session.awaitResponse { it.hasFunctionCall() }
            assertThat(functionResponse.functionCall.functionCall.name)
                .isEqualTo("unknown_function_not_in_registry")
        }

        gigaVoiceAgentMock.verify(0, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should execute backend function via GigaAgent when isBackendFunction is true`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("backend-function-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

            mock.sendResponse(platformFunctionProcessing())
            session.awaitResponse { it.hasPlatformFunctionProcessing() }

            wireMock.awaitPostCall("/functions")

            assertThat(session.receivedResponses.none { it.hasFunctionCall() }).isTrue()
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }

    @Test
    fun `should send error function result to downstream when GigaAgent functions endpoint returns 500`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)

        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")
                )
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("functions-error-test"))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()

            session.sendRequest(audioRequest(speechStart = true))
            mock.awaitRequest { it.hasInput() }

            mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

            wireMock.awaitPostCall("/functions")

            val errorFunctionResult = mock.awaitRequest { it.hasFunctionResult() }
            assertThat(errorFunctionResult.functionResult.functionName).isEqualTo("get_account_balance")
            assertThat(errorFunctionResult.functionResult.content).contains("error")
            assertThat(errorFunctionResult.functionResult.content).contains("500")
        }

        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/functions")))
    }
}
