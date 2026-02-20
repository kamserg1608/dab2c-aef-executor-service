package ru.sbrf.dab2c.executor.application.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

/** Logback converter that outputs all non-blank MDC entries as `[key=value]` pairs, sorted alphabetically. */
class PresentMdcConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent): String {
        val mdc = event.mdcPropertyMap
        if (mdc.isNullOrEmpty()) return ""
        val sb = StringBuilder()
        for ((key, value) in mdc.toSortedMap()) {
            if (!value.isNullOrBlank()) {
                sb.append(" [").append(key).append('=').append(value).append(']')
            }
        }
        return sb.toString()
    }
}
