package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockResponses
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

/**
 * Verifies session initialization when SDS reports no ucp_id: the profile service must not be
 * called and the UCP ID must be absent from everything sent downstream.
 */
class SessionWithoutUcpIdTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should skip person info and omit ucp id when session has none`() = runItTest {
        setupStubs(
            efsAdapterMock,
            gigaVoiceAgentMock,
            sdsSessionBody = WireMockResponses.SDS_DA_SESSION_NO_UCP_ID
        )

        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest("no-ucp-id-call"))

            val settingsCall = wireMock.awaitPostCall("/settings")

            efsAdapterMock.verify(0, postRequestedFor(urlEqualTo("/getPersonInfoByRegionKind")))
            assertFalse(settingsCall.headers.getHeader("Da-Ucp-Id").isPresent)

            val body: Map<String, Any?> = ObjectMappers.MAPPER.readValue(settingsCall.bodyAsString)
            assertMatchesGolden(
                maskNonDeterministic(body, setOf("Da-Request-Id")),
                "golden/settings-request/session-without-ucp-id.json"
            )
        }
    }
}
