package ru.sbrf.dab2c.executor.it.support.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
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

    fun WireMockServer.stubEfsAdapterConfiguratorFunction(
        functionName: String,
        type: String,
        path: String? = null
    ) {
        val pathJson = path?.let { ",\n        \"path\": \"$it\"" } ?: ""

        stubFor(
            post(urlEqualTo("/configurator/function"))
                .withRequestBody(matchingJsonPath("$.functionName", equalTo(functionName)))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                            {
                              "success": true,
                              "body": {
                                "type": "$type"$pathJson,
                                "modality": ["text", "voice"]
                              }
                            }
                            """.trimIndent()
                        )
                )
        )
    }

    fun WireMockServer.stubConfiguratorFunctionList() {
        stubFor(
            post(urlEqualTo("/function/list/v1"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.CONFIGURATOR_FUNCTION_LIST_RESPONSE)
                )
        )
    }

    fun WireMockServer.stubConfiguratorFunctionListIag() {
        stubFor(
            post(urlEqualTo("/bh"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.FIND_BANK_OFFICE_FUNCTION_CALL_RESPONSE)
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

    fun WireMockServer.stubRetrieveParams(response: String) {
        stubFor(
            post(urlEqualTo("/retrieveParams"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)
                )
        )
    }

    fun WireMockServer.stubRetrieveParams(vararg params: Pair<String, String?>) {
        stubRetrieveParams(WireMockResponses.retrieveParamsResponse(*params))
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

    fun WireMockServer.stubGigaAgentSettings(
        withFunctions: Boolean = false,
        configuratorEnabled: Boolean = false
    ) {
        val responseBody = when {
            configuratorEnabled -> {
                WireMockResponses.GIGA_VOICE_SETTINGS_CONFIGURATOR_RESPONSE
            }

            withFunctions -> {
                WireMockResponses.GIGA_VOICE_SETTINGS_WITH_FUNCTIONS_RESPONSE
            }

            else -> {
                WireMockResponses.GIGA_VOICE_SETTINGS_RESPONSE
            }
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

    fun WireMockServer.stubGigaAgentSettingsWithProfanityCheck() {
        stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(WireMockResponses.GIGA_VOICE_SETTINGS_WITH_PROFANITY_CHECK_RESPONSE)
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
            stubPersonInfo()
            stubRetrieveParams()
        }
        gigaAgent.stubGigaAgentSettings(withFunctions)
    }

    fun setupStubsWithKapExtra(
        efsAdapter: WireMockServer,
        gigaAgent: WireMockServer,
        withFunctions: Boolean = false,
    ) {
        with(efsAdapter) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
            stubConfiguratorSession()
            stubEfsAuditEvent()
            stubPersonInfo()
            stubRetrieveParams("aef.executor.toggles.kap.send.extra" to "true")
        }
        gigaAgent.stubGigaAgentSettings(withFunctions)
    }

    fun setupStubsWithFunctionMatch(
        efsAdapter: WireMockServer,
        configurator: WireMockServer,
        gigaAgent: WireMockServer,
        configuratorEnabled: Boolean = true,
        withFunctions: Boolean = true,
    ) {
        with(efsAdapter) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
            stubConfiguratorSession()
            stubEfsAuditEvent()
            stubPersonInfo()
            stubRetrieveParams(
                "aef.executor.toggles.configurator.function-match" to configuratorEnabled.toString()
            )

            if (configuratorEnabled) {
                stubEfsAdapterConfiguratorFunction("transfer_to_operator", "DIVR")
                stubEfsAdapterConfiguratorFunction("get_account_balance", "BACKEND")
                stubEfsAdapterConfiguratorFunction("find_bank_office_iag", "IAG", "/bh")
            }
        }

        if (configuratorEnabled) {
            configurator.stubConfiguratorFunctionList()
        }

        gigaAgent.stubGigaAgentSettings(withFunctions, configuratorEnabled)
    }

    @Suppress("LongParameterList")
    fun setupStubsWithFunctionMatchIag(
        efsAdapter: WireMockServer,
        configurator: WireMockServer,
        gigaAgent: WireMockServer,
        iag: WireMockServer,
        configuratorEnabled: Boolean = true,
        withFunctions: Boolean = true,
    ) {
        with(efsAdapter) {
            stubEfsRestAgent()
            stubSdsSessionReadData()
            stubConfiguratorSession()
            stubEfsAuditEvent()
            stubPersonInfo()
            stubRetrieveParams(
                "aef.executor.toggles.configurator.function-match" to configuratorEnabled.toString()
            )

            if (configuratorEnabled) {
                stubEfsAdapterConfiguratorFunction("transfer_to_operator", "DIVR")
                stubEfsAdapterConfiguratorFunction("get_account_balance", "BACKEND")
                stubEfsAdapterConfiguratorFunction("find_bank_office_iag", "IAG", "/bh")
            }
        }

        if (configuratorEnabled) {
            configurator.stubConfiguratorFunctionList()
            iag.stubConfiguratorFunctionListIag()
        }

        gigaAgent.stubGigaAgentSettings(withFunctions, configuratorEnabled)
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
            stubPersonInfo()
            stubRetrieveParams()
        }
        gigaAgent.stubGigaAgentSettingsWithAnalytics(dataVersion, analyticsData)
    }
}
