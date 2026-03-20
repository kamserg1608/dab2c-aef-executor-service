package ru.sbrf.dab2c.executor.library.time

/** Abstraction over system clock for deterministic testing. */
interface TimeProvider {
    /** Returns current time in milliseconds. */
    fun currentTimeMillis(): Long
}
