package ru.sbrf.dab2c.executor.domain.voice

/**
 * Function call details (shared between request and response).
 */
data class FunctionCall(
    val name: String,
    val arguments: String
)
