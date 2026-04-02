package ru.sbrf.dab2c.executor.application.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import net.logstash.logback.encoder.LogstashEncoder

/** LogstashEncoder that replaces all registered sensitive values in the JSON output. */
class MaskingLogstashEncoder : LogstashEncoder() {

    override fun encode(event: ILoggingEvent): ByteArray {
        val bytes = super.encode(event)
        if (!MaskingConfig.keyMaskingEnabled && !MaskingConfig.regexMaskingEnabled) return bytes
        return applyMasking(String(bytes, Charsets.UTF_8), event.mdcPropertyMap)
            .toByteArray(Charsets.UTF_8)
    }
}
