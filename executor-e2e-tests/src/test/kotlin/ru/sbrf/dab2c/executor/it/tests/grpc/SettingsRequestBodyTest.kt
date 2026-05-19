package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithFunctionMatch
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

/**
 * Verifies that the /settings request body sent to GigaAgent contains
 * correctly serialized user_info and session_info fields. Asserts the
 * whole payload against committed golden files.
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
    @Disabled("temporarily disabled")
    fun `should include session_info with functions when function match enabled`() = runItTest {
        setupStubsWithFunctionMatch(efsAdapterMock, configuratorMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("headers-test-call"))

            val functionCall = wireMock.awaitPostCall("/configurator/function/list/v1")
            @Suppress("UnusedPrivateProperty")
            val bodyFunction: Map<String, Any?> =
                ObjectMappers.MAPPER.readValue(functionCall.bodyAsString)

            val settingsCall = wireMock.awaitPostCall("/settings")
            val body: Map<String, Any?> =
                ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)

            assertMatchesGolden(
                maskNonDeterministic(body, SETTINGS_BODY_NON_DETERMINISTIC_FIELDS),
                "golden/settings-request/session-info-function.json"
            )
        }
    }

    private companion object {
        /** Non-deterministic fields in the /settings request body: the per-call request id. */
        private val SETTINGS_BODY_NON_DETERMINISTIC_FIELDS = setOf("Da-Request-Id")
    }
}
