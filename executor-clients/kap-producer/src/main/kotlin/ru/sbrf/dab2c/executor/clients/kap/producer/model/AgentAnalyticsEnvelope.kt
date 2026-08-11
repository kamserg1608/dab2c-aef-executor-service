package ru.sbrf.dab2c.executor.clients.kap.producer.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Envelope for agent analytics data sent to the KAP agents topic.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AgentAnalyticsEnvelope(
    val version: String,
    val id: String,
    val date: Long,
    val data: String? = null
)
