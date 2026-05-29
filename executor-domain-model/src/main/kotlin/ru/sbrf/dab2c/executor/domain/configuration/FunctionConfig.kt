package ru.sbrf.dab2c.executor.domain.configuration

/**
 * Function configuration response body.
 */
data class FunctionConfig(
    val type: String,
    val path: String?,
    val modality: List<String> = emptyList()
)
