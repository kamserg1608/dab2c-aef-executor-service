package ru.sbrf.dab2c.executor.domain.voice

/**
 * Supported audio encoding formats.
 */
enum class AudioEncoding(
    val value: Int
) {
    /** Encoding not specified. */
    UNSPECIFIED(0),
    /** PCM signed 16-bit little-endian. */
    PCM_S16LE(1),
    /** Opus codec. */
    OPUS(2),
    /** PCM A-law. */
    PCM_ALAW(3)
}
