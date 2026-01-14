package ru.sbrf.dab2c.executor.domain.voice

/**
 * Registry of function performers indicating which system handles each function.
 */
data class FunctionPerformers(
    val functions: Map<String, FunctionOptions> = emptyMap()
)

/**
 * Options for a function indicating its execution context.
 */
data class FunctionOptions(
    val isBackendFunction: Boolean
)
