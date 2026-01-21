package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Envelope for agent analytics data sent to the KAP agents topic.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentAnalyticsEnvelope(
    val version: String,
    val id: String,
    val date: Long,
    @JsonProperty("session_id")
    val sessionId: String,
    @JsonProperty("conversation_id")
    val conversationId: String,
    @JsonProperty("ucp_id")
    val ucpId: String,
    val block: String,
    val channel: String,
    @JsonProperty("agent_name")
    val agentName: String,
    @JsonProperty("data_version")
    val dataVersion: String,
    @JsonProperty("agent_ci")
    val agentCi: String,
    val size: String,
    @JsonProperty("request_id")
    val requestId: String? = null,
    @JsonProperty("app_source")
    val appSource: String? = null,
    val platform: String? = null,
    val data: String? = null
)
