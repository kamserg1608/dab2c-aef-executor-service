package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.properties.api

/**
 * Configuration properties for GigaVoice Agent API client.
 */
interface GigaVoiceAgentClientProperties {

    /**
     * Base URL of the GigaVoice Agent API.
     */
    val baseUrl: String

    /**
     * Connection timeout in milliseconds.
     */
    val connectionTimeout: Long

    /**
     * Request timeout in milliseconds.
     */
    val requestTimeout: Long

}
