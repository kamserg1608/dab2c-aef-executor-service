package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that the /settings request body sent to GigaAgent contains
 * correctly serialized user_info and session_info fields.
 */
class SettingsRequestBodyTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should include user_info with person data in settings request`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("body-test-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val request = ObjectMappers.MAPPER.readValue<GigaVoiceSettingsRequestSchema>(
                settingsCall.bodyAsString
            )

            val userInfo = request.userInfo
            assertThat(userInfo).isNotNull
            assertThat(userInfo!!.firstName).isEqualTo("Всеслав")
            assertThat(userInfo.patrName).isEqualTo("Владиславович")
            assertThat(userInfo.birthDay).isEqualTo("1998-01-20T14:50:00.000+0300")
            assertThat(userInfo.segmentCodeType).isEqualTo("1")
            assertThat(userInfo.ucpId).isEqualTo("2064943548742642674")
        }
    }

    @Test
    fun `should include session_info with headers in settings request`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("headers-test-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")
            val request = ObjectMappers.MAPPER.readValue<GigaVoiceSettingsRequestSchema>(
                settingsCall.bodyAsString
            )

            val sessionInfo = request.sessionInfo
            assertThat(sessionInfo).isNotNull
            assertThat(sessionInfo!!.headers).isNotNull
            assertThat(sessionInfo.headers).containsEntry("UFS-SESSION", "test-session")
            assertThat(sessionInfo.headers).containsEntry("X-Trace-Id", "test-trace-id")
            assertThat(sessionInfo.headers).containsEntry("Da-Channel", "test-channel")
            assertThat(sessionInfo.headers).containsEntry("Da-Platform", "test-platform")
            assertThat(sessionInfo.headers).containsKey("Da-Request-Id")
            assertThat(sessionInfo.headers).containsKey("Da-Session-Id")
            assertThat(sessionInfo.headers).containsKey("Da-Ucp-Id")

            assertThat(sessionInfo.cookies).isNotNull
            assertThat(sessionInfo.cookies).containsEntry("UFS-SESSION", "test-session")
            assertThat(sessionInfo.cookies).containsEntry("UFS-TOKEN", "test-token")
        }
    }
}
