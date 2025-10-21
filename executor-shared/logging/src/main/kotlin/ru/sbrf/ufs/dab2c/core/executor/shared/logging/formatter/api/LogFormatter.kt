package ru.sbrf.ufs.dab2c.core.executor.shared.logging.formatter.api

/**
 * Formatter for log arguments. Converts log argument to formatted string.
 */
interface LogFormatter {

    /**
     * Converts argument to formatted string.
     */
    fun format(obj: Any?): String?
}
