package ru.sbrf.dab2c.executor.domain.voice

/**
 * Audio content from client.
 */
data class AudioContent(
    val audioChunk: ByteArray? = null,
    val speechStart: Boolean = false,
    val speechEnd: Boolean = false,
    val meta: AudioChunkMeta? = null
)

/** Metadata for an audio chunk. */
data class AudioChunkMeta(
    val forceNoSpeech: Boolean = false
)

/**
 * Content type for synthesis.
 */
enum class SynthesisContentType {
    /** Plain text. */
    TEXT,
    /** SSML markup. */
    SSML
}

/**
 * Text/SSML content for synthesis.
 */
data class SynthesisContent(
    val text: String,
    val contentType: SynthesisContentType = SynthesisContentType.TEXT,
    val isFinal: Boolean = false
)

/**
 * Function execution result data.
 */
data class FunctionResultData(
    val content: String,
    val functionName: String? = null
)

/**
 * Context data for conversation context updates.
 */
data class ContextData(
    val content: String
)
