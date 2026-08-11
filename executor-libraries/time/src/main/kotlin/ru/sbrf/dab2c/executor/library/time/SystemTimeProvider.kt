package ru.sbrf.dab2c.executor.library.time

/** Production [TimeProvider] that delegates to [System.currentTimeMillis]. */
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
