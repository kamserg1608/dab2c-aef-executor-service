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
     * GigaVoice Agent settings response.
     */
    val GIGA_VOICE_SETTINGS_RESPONSE = """
        {
            "settings": {
                "voice_call_id": "test-call-123",
                "audio": {}
            },
            "performers": {
                "functions": {}
            }
        }
    """.trimIndent()
}
