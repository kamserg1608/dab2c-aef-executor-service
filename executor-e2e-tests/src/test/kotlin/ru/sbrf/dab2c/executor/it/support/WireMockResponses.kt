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
