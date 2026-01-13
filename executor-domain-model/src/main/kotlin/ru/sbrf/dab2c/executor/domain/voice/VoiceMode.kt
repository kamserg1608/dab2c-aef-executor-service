package ru.sbrf.dab2c.executor.domain.voice

/**
 * Voice processing mode.
 */
enum class VoiceMode {
    /** Mode not specified. */
    UNSPECIFIED,
    /** ASR + GigaChat + TTS pipeline. */
    RECOGNIZE_GIGACHAT_SYNTHESIS,
    /** GigaChat + TTS pipeline. */
    GIGACHAT_SYNTHESIS,
    /** GigaChat only. */
    GIGACHAT,
    /** ASR + TTS pipeline. */
    RECOGNIZE_SYNTHESIS
}
