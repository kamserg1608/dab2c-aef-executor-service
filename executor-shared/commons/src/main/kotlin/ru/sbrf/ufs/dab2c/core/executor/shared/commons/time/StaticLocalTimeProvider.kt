package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Base implementation for LocalTimeProvider.
 */
class StaticLocalTimeProvider(private val currentTimeMs: Long) : LocalTimeProvider {

    override fun now(): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMs), ZoneId.systemDefault())

    override fun currentTimeMillis(): Long = currentTimeMs
}
