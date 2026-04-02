package ru.sbrf.dab2c.executor.application.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import net.logstash.logback.encoder.LogstashEncoder
import ru.sbrf.dab2c.executor.logging.MaskingCollector

/** LogstashEncoder that replaces all registered sensitive values in the JSON output. */
class MaskingLogstashEncoder : LogstashEncoder() {

    override fun encode(event: ILoggingEvent): ByteArray {
        val result = super.encode(event)
        val maskMap = MaskingCollector.fromMdc(event.mdcPropertyMap)
        if (maskMap.isEmpty()) return result
        var json = String(result, Charsets.UTF_8)
        for ((raw, masked) in maskMap) {
            json = json.replace(raw, masked)
        }
        return json.toByteArray(Charsets.UTF_8)
    }
}
