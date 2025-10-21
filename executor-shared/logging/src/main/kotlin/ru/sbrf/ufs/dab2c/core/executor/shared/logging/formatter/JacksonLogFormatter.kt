package ru.sbrf.ufs.dab2c.core.executor.shared.logging.formatter

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.formatter.api.LogFormatter

/**
 * Formatter that converts argument to json via [ObjectMapper].
 */
@Suppress("TooGenericExceptionCaught")
class JacksonLogFormatter(private val mapper: ObjectMapper) : LogFormatter {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun format(obj: Any?): String? {
        if (obj == null) return null

        return try {
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj)
        } catch (e: Exception) {
            log.error("Произошла ошибка при попытке форматировать объект: {}", obj, e)
            obj.toString()
        }
    }
}
