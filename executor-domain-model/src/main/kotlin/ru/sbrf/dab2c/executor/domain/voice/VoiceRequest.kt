package ru.sbrf.dab2c.executor.domain.voice

/**
 * Voice request from client - can be settings, audio content, or function result.
 */
sealed class VoiceRequest {

    /**
     * Initial settings request (must be first).
     */
    data class Settings(val settings: VoiceSettings) : VoiceRequest()

    /**
     * Audio content from client.
     */
    data class Audio(val content: AudioContent) : VoiceRequest()

    /**
     * Text content for synthesis.
     */
    data class TextForSynthesis(val content: SynthesisContent) : VoiceRequest()

    /**
     * Function execution result.
     */
    data class FunctionResult(val result: FunctionResultData) : VoiceRequest()

    /**
     * Context update during conversation.
     */
    data class Context(val context: ContextData) : VoiceRequest()
}

/**
 * Audio content from client.
 */
data class AudioContent(
    val audioChunk: ByteArray? = null,
    val speechStart: Boolean = false,
    val speechEnd: Boolean = false
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
