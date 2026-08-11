package ru.sbrf.dab2c.executor.voice.test

import ru.sbrf.dab2c.executor.library.time.TimeProvider
import java.util.concurrent.atomic.AtomicLong

/** Test [TimeProvider] returning monotonically increasing values for deterministic assertions. */
class IncrementingTimeProvider(start: Long = 1L) : TimeProvider {
    private val counter = AtomicLong(start)
    override fun currentTimeMillis(): Long = counter.getAndIncrement()
}
