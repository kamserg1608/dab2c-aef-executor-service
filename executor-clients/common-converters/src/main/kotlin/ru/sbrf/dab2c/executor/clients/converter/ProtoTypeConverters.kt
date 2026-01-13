package ru.sbrf.dab2c.executor.clients.converter

import com.google.protobuf.ByteString
import com.google.protobuf.Duration
import com.google.protobuf.Timestamp
import java.time.Instant
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration as KotlinDuration

private const val NANOS_PER_SECOND = 1_000_000_000L

/**
 * Common type converters for proto <-> domain mapping.
 * These converters are used by Konvert mappers across client modules.
 */
@Suppress("TooManyFunctions")
object ProtoTypeConverters {

    // ============ ByteString <-> ByteArray ============

    /** Converts protobuf ByteString to ByteArray. */
    fun byteStringToByteArray(bytes: ByteString): ByteArray =
        bytes.toByteArray()

    /** Converts ByteArray to protobuf ByteString. */
    fun byteArrayToByteString(bytes: ByteArray): ByteString =
        ByteString.copyFrom(bytes)

    // ============ Proto Duration <-> Kotlin Duration ============

    /** Converts protobuf Duration to Kotlin Duration. */
    fun protoDurationToKotlinDuration(duration: Duration): KotlinDuration =
        duration.seconds.seconds + duration.nanos.nanoseconds

    /** Converts Kotlin Duration to protobuf Duration. */
    fun kotlinDurationToProtoDuration(duration: KotlinDuration): Duration =
        Duration.newBuilder()
            .setSeconds(duration.inWholeSeconds)
            .setNanos((duration.inWholeNanoseconds % NANOS_PER_SECOND).toInt())
            .build()

    // ============ Proto Duration <-> Long (seconds) ============

    /** Converts protobuf Duration to seconds (Long). */
    fun protoDurationToSeconds(duration: Duration): Long =
        duration.seconds

    /** Converts seconds (Long) to protobuf Duration. */
    fun secondsToProtoDuration(seconds: Long): Duration =
        Duration.newBuilder().setSeconds(seconds).build()

    // ============ Proto Timestamp <-> Instant ============

    /** Converts protobuf Timestamp to Java Instant. */
    fun timestampToInstant(timestamp: Timestamp): Instant =
        Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())

    /** Converts Java Instant to protobuf Timestamp. */
    fun instantToTimestamp(instant: Instant): Timestamp =
        Timestamp.newBuilder()
            .setSeconds(instant.epochSecond)
            .setNanos(instant.nano)
            .build()

    // ============ Numeric conversions ============

    /** Converts Float to Double. */
    fun floatToDouble(value: Float): Double =
        value.toDouble()

    /** Converts Double to Float. */
    fun doubleToFloat(value: Double): Float =
        value.toFloat()

    /** Converts Int to Long. */
    fun intToLong(value: Int): Long =
        value.toLong()

    /** Converts Long to Int. */
    fun longToInt(value: Long): Int =
        value.toInt()
}
