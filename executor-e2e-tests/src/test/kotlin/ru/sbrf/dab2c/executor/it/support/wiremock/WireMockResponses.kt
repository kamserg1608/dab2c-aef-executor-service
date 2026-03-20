package ru.sbrf.dab2c.executor.it.support.wiremock

/**
 * Predefined JSON responses for WireMock stubs in integration tests.
 */
object WireMockResponses {

    /**
     * EFS Adapter response for agent configuration.
     */
    val EFS_ADAPTER_RESPONSE = """
        {
            "success": true,
            "body": {
                "test-agent": {
                    "name": "test-agent",
                    "type": "voice",
                    "functional_subsystem_ci": "test-ci",
                    "description": "Test agent",
                    "entry_points": [],
                    "ufs_service_available": true,
                    "can_access_user_info": true,
                    "tools_meta": [],
                    "neighbours_agent_meta": [],
                    "toggles": {}
                }
            }
        }
    """.trimIndent()

    /**
     * GigaVoice Agent settings response.
     */
    val GIGA_VOICE_SETTINGS_RESPONSE = """
        {
            "settings": {
                "voice_call_id": "test-call-123",
                "audio": {}
            },
            "performers": {
                "functions": []
            }
        }
    """.trimIndent()

    /**
     * GigaVoice Agent settings response with profanity check enabled.
     */
    val GIGA_VOICE_SETTINGS_WITH_PROFANITY_CHECK_RESPONSE = """
        {
            "settings": {
                "voice_call_id": "test-call-123",
                "audio": {},
                "gigachat": {"profanity_check": true}
            },
            "performers": {
                "functions": []
            }
        }
    """.trimIndent()

    /**
     * GigaVoice Agent settings response with function registry.
     * Contains both backend and IVR functions.
     */
    val GIGA_VOICE_SETTINGS_WITH_FUNCTIONS_RESPONSE = """
        {
            "settings": {
                "voice_call_id": "test-call-123",
                "audio": {}
            },
            "performers": {
                "functions": [
                    {"name": "get_account_balance", "is_backend_function": true},
                    {"name": "transfer_to_operator", "is_backend_function": false},
                    {"name": "check_transaction_status", "is_backend_function": true}
                ]
            }
        }
    """.trimIndent()

    /**
     * SDS session read data response from DA-SESSION section.
     */
    @Suppress("MaxLineLength")
    val SDS_DA_SESSION = """
        {
            "success": true,
            "body": [
                {
                    "sectionName": "DA_SESSION",
                    "attributeName": "SESSION_INFO_MOB_BANK",
                    "data": "{\"session_id\":\"test-session-id\",\"user_id\":\"test-user-id\",\"ucp_id\":\"test-ucp-id\",\"ufs_host\":\"test-ufs-host\",\"is_valid\":true}"
                }
            ],
            "errors": null
        }
    """.trimIndent()

    /**
     * SDS session read data response from SESSION section.
     */
    @Suppress("MaxLineLength")
    val SDS_COMMON_DA_CONFIGURATOR = """
        {
            "success": true,
            "body": {
                "block": "sflkc-ift-b3-eag001.sberbank.ru",
                "channel": "ivr",
                "platform": "gsm"
            },
            "messages": []
        }
    """.trimIndent()

    /**
     * EFS Adapter response for audit event endpoint.
     */
    val EFS_AUDIT_EVENT_RESPONSE = """
    {
        "success": true,
        "body": null,
        "errors": null
    }
    """.trimIndent()

    /**
     * Person info response.
     */
    @Suppress("MaxLineLength")
    val PERSON_INFO = """
        {
            "success": true,
            "body": {
                "person": {
                    "surName": "Б.",
                    "firstName": "Всеслав",
                    "patrName": "Владиславович",
                    "creationType": "UDBO",
                    "personDepartment": {
                        "tb": "38",
                        "osb": "9038",
                        "vsp": "1688"
                    },
                    "birthDay": "1998-01-20T14:50:00.000+0300",
                    "gender": "M",
                    "email": "FOO@FOO.RU",
                    "citizenShip": "РОССИЯ",
                    "birthPlace": "Москва",
                    "mobileBankProfile": {
                        "registeredPhonesListFull": {
                            "phone": [
                                {
                                    "phoneNumber": "7980***5850",
                                    "isMainPhone": true,
                                    "status": "ACTIVE",
                                    "mainPhone": true
                                }
                            ]
                        }
                    },
                    "phoneList": {
                        "phone": [
                            {
                                "phoneType": "MOBILE",
                                "number": "7980***5850"
                            }
                        ]
                    },
                    "documentList": {
                        "document": [
                            {
                                "documentType": {
                                    "id": 21,
                                    "description": "Паспорт гражданина РФ",
                                    "name": "Паспорт гражданина Российской Федерации"
                                },
                                "series": "41 22",
                                "number": "****97",
                                "main": true,
                                "documentIssueDate": "2022-03-09T14:50:00.000+0300",
                                "documentIssueBy": "ТП № 116 ОУФМС РОССИИ ПО САНКТ-ПЕТЕРБУРГУ И ЛЕНИНГРАДСКОЙ ОБЛ. В КИРОВСКОМ РАЙОНЕ",
                                "documentIssueByCode": "470-032",
                                "documentTimeUpDate": "2043-01-20T00:00:00.000+0300",
                                "documentIdentify": true
                            }
                        ]
                    },
                    "addressList": {
                        "address": [
                            {
                                "type": "REGISTRATION",
                                "postalCode": "119019",
                                "province": "МОСКВА",
                                "city": "МОСКВА",
                                "street": "****ВАЯ",
                                "house": "18",
                                "flat": "20",
                                "unparseableAddress": "******* ******* ****** ** * ******* ** ******** * ** ** **"
                            }
                        ]
                    },
                    "serviceInfo": {
                        "agreementNumber": "380060356439",
                        "agreementDate": "2025-03-27T00:00:00.000+0300",
                        "lastUpdateDate": "2026-02-10T14:07:34.553+0300",
                        "managerDepartmentInfo": {}
                    },
                    "additionalInfo": {
                        "isResident": true,
                        "mdmState": "NOT_SENT",
                        "segmentCodeType": "1",
                        "tariff": {
                            "tarifPlanCodeType": "0",
                            "tarifPlanConnectionDate": "2026-02-10T00:00:00.000+0300"
                        }
                    },
                    "optionList": {
                        "options": []
                    },
                    "papers": {},
                    "ucpId": "2064943548742642674",
                    "partyToPartyGroups": [
                        {
                            "version": 0,
                            "updateDateTime": "2025-03-27T17:35:45.792+0300",
                            "partyGroup": {
                                "code": 95
                            }
                        }
                    ],
                    "isPrivateBanking": false
                }
            },
            "messages": []
        }
    """.trimIndent()

    fun retrieveParamsResponse(vararg params: Pair<String, String?>): String {
        val paramEntries = params.joinToString(",") { (name, value) ->
            if (value != null) {
                """{"name":"$name","type":"STRING","values":["$value"]}"""
            } else {
                """{"name":"$name","type":"EMPTY","values":[]}"""
            }
        }
        return """{"success":true,"body":{"parameters":[$paramEntries]}}"""
    }

    /**
     * GigaVoice Agent settings response with agent analytics.
     */
    fun gigaVoiceSettingsWithAnalyticsResponse(
        dataVersion: String,
        analyticsData: String,
        withFunctions: Boolean = false
    ): String {
        val functions = if (withFunctions) {
            """
                {"name": "get_account_balance", "is_backend_function": true},
                {"name": "transfer_to_operator", "is_backend_function": false},
                {"name": "check_transaction_status", "is_backend_function": true}
            """.trimIndent()
        } else {
            ""
        }
        return """
            {
                "settings": {
                    "voice_call_id": "test-call-123",
                    "audio": {}
                },
                "performers": {
                    "functions": [$functions]
                },
                "agent_analytics": [
                    {
                        "data_version": "$dataVersion",
                        "data": $analyticsData
                    }
                ]
            }
        """.trimIndent()
    }

    /**
     * GigaVoice Agent /functions endpoint response.
     */
    fun gigaVoiceFunctionsResponse(functionName: String, resultContent: String): String {
        val escapedContent = resultContent.replace("\"", "\\\"")
        return """
            {
                "function_result": {
                    "content": "$escapedContent",
                    "function_name": "$functionName"
                }
            }
        """.trimIndent()
    }

    /**
     * GigaVoice Agent /functions endpoint response with agent analytics.
     */
    fun gigaVoiceFunctionsWithAnalyticsResponse(
        functionName: String,
        resultContent: String,
        dataVersion: String,
        analyticsData: String
    ): String {
        val escapedContent = resultContent.replace("\"", "\\\"")
        return """
            {
                "function_result": {
                    "content": "$escapedContent",
                    "function_name": "$functionName"
                },
                "agent_analytics": [
                    {
                        "data_version": "$dataVersion",
                        "data": $analyticsData
                    }
                ]
            }
        """.trimIndent()
    }
}
