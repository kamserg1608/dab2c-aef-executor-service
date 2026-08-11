package ru.sbrf.dab2c.executor.logging

/** Static holder for additional MDC key-value pairs injected into every logging context. */
object AdditionalMdcConfig {

    @Volatile
    var keys: Map<String, String> = emptyMap()
        private set

    /** Sets the additional MDC key-value pairs to be injected into every logging context. */
    fun configure(keys: Map<String, String>) {
        this.keys = keys
    }
}
