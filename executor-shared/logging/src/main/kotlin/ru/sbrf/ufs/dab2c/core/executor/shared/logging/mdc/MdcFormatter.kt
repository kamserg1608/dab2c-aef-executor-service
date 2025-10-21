package ru.sbrf.ufs.dab2c.core.executor.shared.logging.mdc

/**
 * Helper class for formatting mdc variables.
 */
object MdcFormatter {

    /**
     * Format mdc variables with format [mdcName=mdcValue].
     */
    fun formatMdc(mdcName: String, mdcValue: Any?): String? {
        if (mdcValue == null) return null
        return "[$mdcName=$mdcValue]"
    }

    /**
     * Convert map to MDC formatted map.
     */
    fun toMdcParamsMap(paramsMap: Map<String, String>): Map<String, String> =
        paramsMap.mapValues { (key, value) -> formatMdc(key, value).toString() }
}
