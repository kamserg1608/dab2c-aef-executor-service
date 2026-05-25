package ru.sbrf.dab2c.executor.clients.iag.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode

/**
 * EFS-style wrapper returned by IAG function call endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class IagFunctionResponse(
    val success: Boolean? = null,
    val body: JsonNode? = null,
    val context: JsonNode? = null,
    val messages: JsonNode? = null,
    val error: JsonNode? = null,
    val alerts: JsonNode? = null
)
