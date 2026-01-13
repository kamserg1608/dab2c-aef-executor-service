package ru.sbrf.dab2c.executor.domain.voice

import kotlin.time.Duration

/**
 * Voice response from model - can be content, function call, transcription, etc.
 */
sealed class VoiceResponse {

    /**
     * Content output from model (audio, additional data, or interruption).
     */
    data class Output(val content: ContentFromModel) : VoiceResponse()

    /**
     * Function call request from model.
     */
    data class FunctionCalling(val data: FunctionCallingData) : VoiceResponse()

    /**
     * Input transcription (ASR result).
     */
    data class InputTranscription(val transcription: InputTranscriptionData) : VoiceResponse()

    /**
     * Output transcription (what model said).
     */
    data class OutputTranscription(val transcription: OutputTranscriptionData) : VoiceResponse()

    /**
     * Error response.
     */
    data class Error(val error: ErrorData) : VoiceResponse()

    /**
     * Warning response.
     */
    data class Warning(val warning: WarningData) : VoiceResponse()
}

/**
 * Content from model - audio, additional data, or interruption signal.
 */
sealed class ContentFromModel {

    /**
     * Audio output.
     */
    data class Audio(val audio: AudioOutput) : ContentFromModel()

    /**
     * Additional data (usage stats, model info).
     */
    data class AdditionalData(val data: AdditionalDataContent) : ContentFromModel()

    /**
     * Interruption signal.
     */
    data object Interrupted : ContentFromModel()
}

/**
 * Audio output data.
 */
data class AudioOutput(
    val audioChunk: ByteArray,
    val audioDuration: Duration? = null,
    val isFinal: Boolean = false
)

/**
 * Additional data from model.
 */
data class AdditionalDataContent(
    val usage: UsageData? = null,
    val gigachatModelInfo: GigaChatModelInfo? = null,
    val finishReason: String? = null
)

/**
 * Token usage statistics.
 */
data class UsageData(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val precachedPromptTokens: Int = 0
)

/**
 * GigaChat model information.
 */
data class GigaChatModelInfo(
    val name: String,
    val version: String
)

/**
 * Function calling data (wraps function call with timestamp).
 */
data class FunctionCallingData(
    val functionCall: FunctionCall,
    val timestamp: Long
)

/**
 * Input transcription (ASR) data.
 */
data class InputTranscriptionData(
    val text: String,
    val timestamp: Long,
    val unnormalizedText: String? = null,
    val personIdentity: PersonIdentity? = null,
    val prefetch: Boolean = false,
    val whisper: Boolean = false,
    val emotion: Emotion? = null
)

/**
 * Person identity detection result.
 */
data class PersonIdentity(
    val age: AgeType,
    val gender: GenderType,
    val ageScore: Float,
    val genderScore: Float
)

enum class AgeType {
    NONE,
    CHILD,
    ADULT
}

enum class GenderType {
    NONE,
    MALE,
    FEMALE
}

/**
 * Emotion detection result.
 */
data class Emotion(
    val positive: Float,
    val neutral: Float,
    val negative: Float
)

/**
 * Output transcription data.
 */
data class OutputTranscriptionData(
    val text: String,
    val functionsStateId: String,
    val finishReason: String,
    val timestamp: Long
)

/**
 * Error data.
 */
data class ErrorData(
    val status: Int,
    val message: String
)

/**
 * Warning data.
 */
data class WarningData(
    val message: String
)
