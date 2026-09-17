package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Function
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionRanker
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithFunctionMatch
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

/**
 * Asserts the whole /settings request body sent to GigaAgent against committed golden files.
 */
class SettingsRequestBodyTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should include user_info with person data in settings request`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("body-test-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val body: Map<String, Any?> = ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)

            assertMatchesGolden(
                maskNonDeterministic(body, SETTINGS_BODY_NON_DETERMINISTIC_FIELDS),
                "golden/settings-request/user-info.json"
            )
        }
    }

    @Test
    fun `should include session_info with headers in settings request`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("headers-test-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val body: Map<String, Any?> = ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)

            assertMatchesGolden(
                maskNonDeterministic(body, SETTINGS_BODY_NON_DETERMINISTIC_FIELDS),
                "golden/settings-request/session-info.json"
            )
        }
    }

    @Test
    fun `should include session_info with functions when function match enabled`() = runItTest {
        setupStubsWithFunctionMatch(efsAdapterMock, configuratorMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequestWithGigachat("headers-test-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val body: Map<String, Any?> =
                ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)

            assertMatchesGolden(
                maskNonDeterministic(body, SETTINGS_BODY_NON_DETERMINISTIC_FIELDS),
                "golden/settings-request/session-info-function.json"
            )
        }
    }

    @Test
    fun `should resolve settings when configurator omits interruption flags and optional sections`() = runItTest {
        setupStubsWithFunctionMatch(
            efsAdapterMock,
            configuratorMock,
            gigaVoiceAgentMock,
            functionListBody = WireMockResponses.CONFIGURATOR_FUNCTION_LIST_MINIMAL_RESPONSE
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequestWithGigachat("minimal-configurator-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val body: Map<String, Any?> = ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)

            assertMatchesGolden(
                maskNonDeterministic(body, SETTINGS_BODY_NON_DETERMINISTIC_FIELDS),
                "golden/settings-request/session-info-function-minimal.json"
            )
        }
    }

    private fun settingsRequestWithGigachat(voiceCallId: String): GigaVoiceRequest =
        settingsRequest(voiceCallId) {
            setGigachat(
                GigaChatSettings.newBuilder()
                    .setModel("GigaChat-3-Ultra-preview")
                    .setTemperature(0.7f)
                    .setCurrentTime(1789395661)
                    .addFilterStubPhrases("Секундочку")
                    .addFunctions(Function.newBuilder().setName("ivr_client_function").build())
                    .setFunctionRanker(
                        FunctionRanker.newBuilder()
                            .setEnabled(true)
                            .setTopN(5)
                            .setEmbedderModel("ivr-embedder")
                            .build()
                    )
                    .build()
            )
        }

    private companion object {
        /** Non-deterministic fields in the /settings request body: the per-call request id. */
        private val SETTINGS_BODY_NON_DETERMINISTIC_FIELDS = setOf("Da-Request-Id")
    }
}
