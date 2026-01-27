package ru.sbrf.dab2c.executor.it.tests.grpc.kap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.IvrRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.IvrRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.IvrRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.withConsumer
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupFullModeStubsWithAnalytics
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubEfsRestAgent
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctionsWithAnalytics
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithAnalytics
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubSdsSessionReadData
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

private const val AGENTS_TOPIC = "dab2c-agents"

/**
 * Integration tests for agent analytics publishing to KAP through the voice executor flow.
 */
class AnalyticsPublishingIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should publish analytics to KAP when settings response contains analytics`() = runItTest {
        val testAnalyticsData = """{"metric":"test-value","count":123}"""

        setupFullModeStubsWithAnalytics(
            efsAdapterMock,
            gigaVoiceAgentMock,
            "2.0.0",
            testAnalyticsData
        )

        val analyticsRecords = embeddedKafkaBroker.withConsumer<AgentAnalyticsEnvelope>(
            topic = AGENTS_TOPIC,
            filter = { it.data == testAnalyticsData }
        ) {
            runItTest {
                withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest())

                    mock.awaitRequest { it.hasSettings() }
                }
            }
        }

        assertThat(analyticsRecords).isNotEmpty()

        val receivedAnalytics = analyticsRecords.first()
        assertThat(receivedAnalytics.version).isEqualTo("1.2.0")
        assertThat(receivedAnalytics.data).isEqualTo(testAnalyticsData)
    }

    @Test
    fun `should publish analytics to KAP when function response contains analytics`() = runItTest {
        val testAnalyticsData = """{"function_metric":"function-value","execution_time":42}"""

        with(efsAdapterMock) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
        }
        gigaVoiceAgentMock.stubGigaAgentSettingsWithAnalytics("1.0.0", """{"init":"data"}""", withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctionsWithAnalytics(
            functionName = "get_account_balance",
            resultContent = """{"balance": 1000}""",
            dataVersion = "3.0.0",
            analyticsData = testAnalyticsData
        )

        val analyticsRecords = embeddedKafkaBroker.withConsumer<AgentAnalyticsEnvelope>(
            topic = AGENTS_TOPIC,
            filter = { it.data == testAnalyticsData }
        ) {
            runItTest {
                withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest())

                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(outputTranscriptionResponse())
                    session.awaitResponse()

                    session.sendRequest(audioRequest(speechStart = true))
                    mock.awaitRequest { it.hasInput() }

                    mock.sendResponse(functionCallingResponse("get_account_balance", """{"account_id": "12345"}"""))

                    wireMock.awaitPostCall("/functions")
                }
            }
        }

        assertThat(analyticsRecords).isNotEmpty()

        val receivedAnalytics = analyticsRecords.first()
        assertThat(receivedAnalytics.version).isEqualTo("1.2.0")
        assertThat(receivedAnalytics.data).isEqualTo(testAnalyticsData)
    }
}
