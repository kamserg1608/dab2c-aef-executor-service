package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * A class for representing an exception with an error code, text, and header.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    /**
     * Indicates whether the operation was successful.
     *
     * This property defaults to false, meaning that if no value is provided,
     * it will be assumed that the operation was not successful.
     */
    @JsonProperty("success")
    val success: Boolean = false,
    /**
     * A list of errors that occurred during the operation.
     *
     * This property is required and cannot be null. It should contain.
     *
     */
    @JsonProperty("errors")
    val errors: List<String>,
)
