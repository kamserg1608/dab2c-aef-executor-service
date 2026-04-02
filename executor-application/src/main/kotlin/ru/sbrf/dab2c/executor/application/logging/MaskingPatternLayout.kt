package ru.sbrf.dab2c.executor.application.logging

import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.spi.ILoggingEvent

/** Logback layout that replaces all registered sensitive values in the final formatted output. */
class MaskingPatternLayout : PatternLayout() {

    override fun doLayout(event: ILoggingEvent): String =
        applyMasking(super.doLayout(event), event.mdcPropertyMap)
}
