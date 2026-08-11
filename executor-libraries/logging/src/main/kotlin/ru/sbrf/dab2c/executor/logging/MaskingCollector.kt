package ru.sbrf.dab2c.executor.logging

import org.slf4j.MDC

/**
 * Collects sensitive values for the current request scope.
 * Backed by MDC for automatic coroutine propagation via kotlinx-coroutines-slf4j.
 *
 * Business code registers raw values; a Logback Layout reads the masking map
 * and replaces all occurrences in every log message automatically.
 */
object MaskingCollector {

    const val MDC_KEY = "__maskingValues"
    private const val SEP = "§"

    /** Register one or more sensitive values for masking in all subsequent log output. */
    fun register(vararg rawValues: String) {
        val current = currentMap().toMutableMap()
        rawValues.forEach { raw ->
            if (raw.isNotBlank()) current[raw] = mask(raw)
        }
        if (current.isNotEmpty()) MDC.put(MDC_KEY, encode(current))
    }

    /** Extract masking map from an MDC property map (used by Logback components). */
    fun fromMdc(mdcMap: Map<String, String>?): Map<String, String> {
        val encoded = mdcMap?.get(MDC_KEY) ?: return emptyMap()
        return decode(encoded)
    }

    private fun currentMap(): Map<String, String> {
        val encoded = MDC.get(MDC_KEY) ?: return emptyMap()
        return decode(encoded)
    }

    private fun encode(map: Map<String, String>): String =
        map.entries.joinToString(SEP) { "${it.key}$SEP${it.value}" }

    private fun decode(encoded: String): Map<String, String> =
        encoded.split(SEP).chunked(2) { (raw, masked) -> raw to masked }.toMap()
}

/** Masks a value: first char + `*` × (length - 2) + last char. Values ≤ 2 chars become all `*`. */
fun mask(value: String): String = if (value.length <= 2) {
    "*".repeat(value.length)
} else {
    "${value.first()}${"*".repeat(value.length - 2)}${value.last()}"
}
