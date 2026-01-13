package ru.sbrf.dab2c.executor.clients.converter

import com.google.protobuf.ByteString
import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import java.time.Instant
import kotlin.time.Duration as KotlinDuration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Common type converters for proto <-> domain mapping.
 * These converters are used by Konvert mappers across client modules.
 */
object ProtoTypeConverters {

    // ============ ByteString <-> ByteArray ============

    fun byteStringToByteArray(bytes: ByteString): ByteArray =
        bytes.toByteArray()

    fun byteArrayToByteString(bytes: ByteArray): ByteString =
        ByteString.copyFrom(bytes)

    // ============ Proto Duration <-> Kotlin Duration ============

    fun protoDurationToKotlinDuration(duration: Duration): KotlinDuration =
        duration.seconds.seconds + duration.nanos.nanoseconds

    fun kotlinDurationToProtoDuration(duration: KotlinDuration): Duration =
        Duration.newBuilder()
            .setSeconds(duration.inWholeSeconds)
            .setNanos((duration.inWholeNanoseconds % 1_000_000_000).toInt())
            .build()

    // ============ Proto Duration <-> Long (seconds) ============

    fun protoDurationToSeconds(duration: Duration): Long =
        duration.seconds

    fun secondsToProtoDuration(seconds: Long): Duration =
        Duration.newBuilder().setSeconds(seconds).build()

    // ============ Proto Timestamp <-> Instant ============

    fun timestampToInstant(timestamp: Timestamp): Instant =
        Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())

    fun instantToTimestamp(instant: Instant): Timestamp =
        Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()

    // ============ Numeric conversions ============

    fun floatToDouble(value: Float): Double =
        value.toDouble()

    fun doubleToFloat(value: Double): Float =
        value.toFloat()

    fun intToLong(value: Int): Long =
        value.toLong()

    fun longToInt(value: Long): Int =
        value.toInt()
}
