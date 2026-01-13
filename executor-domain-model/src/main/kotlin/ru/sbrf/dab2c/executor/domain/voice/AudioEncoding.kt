package ru.sbrf.dab2c.executor.domain.voice

/**
 * Supported audio encoding formats.
 */
enum class AudioEncoding {
    /** Encoding not specified. */
    UNSPECIFIED,
    /** PCM signed 16-bit little-endian. */
    PCM_S16LE,
    /** Opus codec. */
    OPUS,
    /** PCM A-law. */
    PCM_ALAW
}
