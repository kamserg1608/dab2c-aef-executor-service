package ru.sbrf.ufs.dab2c.core.executor.shared.commons.time

import java.time.LocalDateTime

/**
 * Interface for local time provider.
 */
interface LocalTimeProvider {
    /**
     * Returns current local date time.
     *
     * @return current local date time
     */
    fun now(): LocalDateTime

    /**
     * Returns current local date time in milliseconds.
     *
     * @return current local date time in milliseconds
     */
    fun currentTimeMillis(): Long
}
