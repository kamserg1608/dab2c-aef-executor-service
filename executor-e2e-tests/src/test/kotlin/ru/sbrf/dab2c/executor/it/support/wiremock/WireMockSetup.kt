package ru.sbrf.dab2c.executor.it.support.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo

object WireMockSetup {

    fun WireMockServer.stubEfsRestAgent() {
        stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_ADAPTER_RESPONSE)
                )
        )
    }

    fun WireMockServer.stubSdsSessionReadData() {
        stubFor(
            post(urlEqualTo("/session/readData"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.SDS_DA_SESSION)
                )
        )
    }

    fun WireMockServer.stubConfiguratorSession() {
        stubFor(
            post(urlEqualTo("/configurator/session"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.SDS_COMMON_DA_CONFIGURATOR)
                )
        )
    }

    fun WireMockServer.stubPersonInfo() {
        stubFor(
            post(urlEqualTo("/getPersonInfoByRegionKind"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.PERSON_INFO)
                )
        )
    }

    fun WireMockServer.stubEfsAuditEvent() {
        stubFor(
            post(urlEqualTo("/audit/event"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.EFS_AUDIT_EVENT_RESPONSE)
                )
        )
    }

    fun WireMockServer.stubGigaAgentSettings(withFunctions: Boolean = false) {
        val responseBody = if (withFunctions) {
            WireMockResponses.GIGA_VOICE_SETTINGS_WITH_FUNCTIONS_RESPONSE
        } else {
            WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE
        }
        stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }

    fun WireMockServer.stubGigaAgentSettingsWithDelay(delayMs: Int, withFunctions: Boolean = false) {
        val responseBody = if (withFunctions) {
            WireMockResponses.GIGA_VOICE_SETTINGS_WITH_FUNCTIONS_RESPONSE
        } else {
            WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE
        }
        stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(delayMs)
                        .withBody(responseBody)
                )
        )
    }

    fun WireMockServer.stubGigaAgentSettingsWithAnalytics(
        dataVersion: String,
        analyticsData: String,
        withFunctions: Boolean = false
    ) {
        stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            WireMockResponses.gigaVoiceSettingsWithAnalyticsResponse(
                                dataVersion, analyticsData, withFunctions
                            )
                        )
                )
        )
    }

    fun WireMockServer.stubGigaAgentFunctions(functionName: String, resultContent: String) {
        stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.gigaVoiceFunctionsResponse(functionName, resultContent))
                )
        )
    }

    fun WireMockServer.stubGigaAgentFunctionsWithDelay(
        functionName: String,
        resultContent: String,
        delayMs: Int,
    ) {
        stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withFixedDelay(delayMs)
                        .withBody(WireMockResponses.gigaVoiceFunctionsResponse(functionName, resultContent))
                )
        )
    }

    fun WireMockServer.stubGigaAgentFunctionsWithAnalytics(
        functionName: String,
        resultContent: String,
        dataVersion: String,
        analyticsData: String
    ) {
        stubFor(
            post(urlEqualTo("/functions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            WireMockResponses.gigaVoiceFunctionsWithAnalyticsResponse(
                                functionName, resultContent, dataVersion, analyticsData
                            )
                        )
                )
        )
    }

    fun setupStubs(
        efsAdapter: WireMockServer,
        gigaAgent: WireMockServer,
        withFunctions: Boolean = false,
    ) {
        with(efsAdapter) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
            stubConfiguratorSession()
            stubEfsAuditEvent()
        }
        gigaAgent.stubGigaAgentSettings(withFunctions)
    }

    fun setupStubsWithAnalytics(
        efsAdapter: WireMockServer,
        gigaAgent: WireMockServer,
        dataVersion: String,
        analyticsData: String
    ) {
        with(efsAdapter) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
            stubConfiguratorSession()
            stubEfsAuditEvent()
        }
        gigaAgent.stubGigaAgentSettingsWithAnalytics(dataVersion, analyticsData)
    }
}
