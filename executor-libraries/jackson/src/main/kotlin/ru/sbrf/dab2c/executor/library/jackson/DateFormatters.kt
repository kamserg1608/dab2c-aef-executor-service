package ru.sbrf.dab2c.executor.library.jackson

import java.time.format.DateTimeFormatter

/**
 * Shared date formatters for the application.
 */
object DateFormatters {

    val DEFAULT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}
