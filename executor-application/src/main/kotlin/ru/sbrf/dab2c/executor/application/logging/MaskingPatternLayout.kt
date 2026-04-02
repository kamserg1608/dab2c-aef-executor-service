package ru.sbrf.dab2c.executor.application.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent
import ru.sbrf.dab2c.executor.logging.MaskingCollector

/** Logback layout that replaces all registered sensitive values in the final formatted output. */
class MaskingPatternLayout : PatternLayout() {

    override fun doLayout(event: ILoggingEvent): String {
        var result = super.doLayout(event)
        val maskMap = MaskingCollector.fromMdc(event.mdcPropertyMap)
        for ((raw, masked) in maskMap) {
            result = result.replace(raw, masked)
        }
        return result
    }
}
