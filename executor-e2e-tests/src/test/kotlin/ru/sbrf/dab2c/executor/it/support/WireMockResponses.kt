package ru.sbrf.dab2c.executor.it.support

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
     * EFS Adapter response for session configuration.
     */
    val EFS_SESSION_CONFIG_RESPONSE = """
        {
            "success": true,
            "body": {
                "channel": "MOB_BANK",
                "platform": "android"
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
     * SDS session read data response with DA session info.
     */
    @Suppress("MaxLineLength")
    val SDS_SESSION_READ_DATA_RESPONSE = """
        {
            "result": [
                {
                    "sectionName": "DA_SESSION",
                    "attributeName": "SESSION_INFO_MOB_BANK",
                    "data": "{\"meta\":{\"session_id\":\"test-session-id\",\"user_id\":\"test-user-id\",\"ucp_id\":\"test-ucp-id\",\"ufs_host\":\"test-ufs-host\",\"is_valid\":true},\"common\":{\"block\":\"retail\",\"channel\":\"MOB_BANK\",\"surface\":\"mobile\",\"platform\":\"android\",\"sdk_version\":\"1.0.0\",\"entry_point\":\"main\",\"app_version\":\"2.0.0\",\"channel_version\":\"3.0.0\",\"app_source\":\"store\",\"time_zone\":\"Europe/Moscow\"},\"user_info\":{\"first_name\":\"Test\",\"patr_name\":\"User\",\"birth_day\":\"1990-01-01\",\"segment_code_type\":\"MASS\",\"ucp_id\":\"test-ucp-id\"}}"
                }
            ],
            "errors": null
        }
    """.trimIndent()

    /**
     * GigaVoice Agent /functions endpoint response.
     */
    fun gigaVoiceFunctionsResponse(functionName: String, resultContent: String) = """
        {
            "function_result": {
                "content": $resultContent,
                "function_name": "$functionName"
            }
        }
    """.trimIndent()
}
