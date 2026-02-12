package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Enriched agent analytics data containing session/request context metadata.
 */
data class AgentAnalyticsData(
    @JsonProperty("session_id") val sessionId: String,
    @JsonProperty("conversation_id") val conversationId: String,
    @JsonProperty("request_id") val requestId: String,
    @JsonProperty("ucp_id") val ucpId: String,
    val block: String,
    val channel: String,
    @JsonProperty("app_source") val appSource: String,
    val platform: String,
    @JsonProperty("agent_name") val agentName: String,
    @JsonProperty("agent_ci") val agentCi: String,
    val size: String,
    @JsonProperty("data_version") val dataVersion: String,
    val data: String
)
