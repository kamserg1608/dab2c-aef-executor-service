package ru.sbrf.dab2c.executor.clients.converter

import com.google.protobuf.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration as KotlinDuration

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Common type converters for proto <-> domain mapping.
 */
object ProtoTypeConverters {

    /** Converts protobuf Duration to Kotlin Duration. */
    fun protoDurationToKotlinDuration(duration: Duration): KotlinDuration =
        duration.seconds.seconds + duration.nanos.nanoseconds

    /** Converts Kotlin Duration to protobuf Duration. */
    fun kotlinDurationToProtoDuration(duration: KotlinDuration): Duration =
        Duration.newBuilder()
            .setSeconds(duration.inWholeSeconds)
            .setNanos((duration.inWholeNanoseconds % NANOS_PER_SECOND).toInt())
            .build()
}
