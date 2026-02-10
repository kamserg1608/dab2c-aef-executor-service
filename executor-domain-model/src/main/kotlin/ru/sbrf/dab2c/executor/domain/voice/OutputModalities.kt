package ru.sbrf.dab2c.executor.domain.voice

/**
 * Output modalities for voice responses.
 */
enum class OutputModalities(
    val value: Int
) {
    /** Modality not specified. */
    UNSPECIFIED(0),
    /** Audio only output. */
    AUDIO(1),
    /** Audio and text output. */
    AUDIO_TEXT(2),
    /** Text only output. */
    TEXT(3)
}
