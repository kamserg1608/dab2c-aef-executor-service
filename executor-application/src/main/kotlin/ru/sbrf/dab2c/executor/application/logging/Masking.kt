package ru.sbrf.dab2c.executor.application.logging

import ru.sbrf.dab2c.executor.logging.MaskingCollector

private const val REGEX_REPLACEMENT = "***"

/** Applies key-based and regex-based masking to [text] according to current [MaskingConfig] settings. */
fun applyMasking(text: String, mdcMap: Map<String, String>?): String {
    var result = text
    if (MaskingConfig.keyMaskingEnabled) {
        for ((raw, masked) in MaskingCollector.fromMdc(mdcMap)) {
            result = result.replace(raw, masked)
        }
    }
    if (MaskingConfig.regexMaskingEnabled) {
        for (pattern in MaskingConfig.compiledPatterns) {
            result = pattern.replace(result, REGEX_REPLACEMENT)
        }
    }
    return result
}
