package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import java.math.BigDecimal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Type converters for mapping between domain and API types.
 */
object TypeConverters {

    /**
     * Converts Duration to String format ("30s").
     */
    fun durationToString(duration: Duration?): String? =
        duration?.let { "${it.inWholeSeconds}s" }

    /**
     * Converts String format ("30s") to Duration.
     */
    fun stringToDuration(value: String?): Duration? =
        value?.removeSuffix("s")?.toLongOrNull()?.seconds

    /**
     * Converts Float to BigDecimal.
     */
    fun floatToBigDecimal(value: Float?): BigDecimal? =
        value?.let { BigDecimal.valueOf(it.toDouble()) }

    /**
     * Converts BigDecimal to Float.
     */
    fun bigDecimalToFloat(value: BigDecimal?): Float? =
        value?.toFloat()
}
