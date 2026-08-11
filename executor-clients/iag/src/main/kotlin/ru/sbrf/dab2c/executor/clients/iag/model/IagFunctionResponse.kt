package ru.sbrf.dab2c.executor.clients.iag.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode

/**
 * Response returned by IAG function call endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionResponse(
    val content: IagFunctionContent? = null,
    @JsonProperty("x_context")
    val xContext: IagFunctionContext? = null,
    @JsonProperty("x_analytics")
    val xAnalytics: List<IagAnalyticsEnvelope> = emptyList()
)

/**
 * Function call response content.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionContent(
    @JsonProperty("function_result")
    val functionResult: IagFunctionResult? = null
)

/**
 * Function execution result.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionResult(
    val name: String? = null,
    val content: JsonNode? = null
)

/**
 * Additional IAG response context.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionContext(
    val error: IagFunctionError? = null
)

/**
 * IAG error payload.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionError(
    val type: String? = null,
    val label: String? = null,
    val description: String? = null,
    val payload: JsonNode? = null
)

/**
 * Analytics wrapper returned by IAG.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagAnalyticsEnvelope(
    @JsonProperty("agent_analytics")
    val agentAnalytics: List<IagAgentAnalytics> = emptyList()
)

/**
 * Analytics event returned by IAG.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagAgentAnalytics(
    @JsonProperty("data_version")
    val dataVersion: String,
    val data: JsonNode
)
