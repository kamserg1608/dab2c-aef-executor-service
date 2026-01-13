package ru.sbrf.dab2c.executor.domain.voice

/**
 * Output modalities for voice responses.
 */
enum class OutputModalities {
    /** Modality not specified. */
    UNSPECIFIED,
    /** Audio only output. */
    AUDIO,
    /** Audio and text output. */
    AUDIO_TEXT,
    /** Text only output. */
    TEXT
}
