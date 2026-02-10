package ru.sbrf.dab2c.executor.domain.voice

/**
 * Voice processing mode.
 */
@Suppress("MagicNumber")
enum class VoiceMode(
    val value: Int
) {
    /** Mode not specified. */
    UNSPECIFIED(0),
    /** ASR + GigaChat + TTS pipeline. */
    RECOGNIZE_GIGACHAT_SYNTHESIS(1),
    /** GigaChat + TTS pipeline. */
    GIGACHAT_SYNTHESIS(2),
    /** GigaChat only. */
    GIGACHAT(3),
    /** ASR + TTS pipeline. */
    RECOGNIZE_SYNTHESIS(4)
}
