package ru.sbrf.dab2c.executor.application.logging

/**
 * Static holder for masking configuration, accessible from both Spring beans and
 * Logback components (which are not Spring-managed).
 *
 * Populated once at startup via [MaskingConfiguration]; safe for Logback to read
 * before Spring initialises (defaults are no-op).
 */
object MaskingConfig {

    @Volatile
    var keyMaskingEnabled: Boolean = true
        private set

    @Volatile
    var regexMaskingEnabled: Boolean = true
        private set

    @Volatile
    var compiledPatterns: List<Regex> = emptyList()
        private set

    /** Initialises masking toggles and compiles regex patterns. Called once at startup. */
    fun configure(keyEnabled: Boolean, regexEnabled: Boolean, patterns: List<String>) {
        keyMaskingEnabled = keyEnabled
        regexMaskingEnabled = regexEnabled
        compiledPatterns = patterns.map { it.toRegex() }
    }
}
