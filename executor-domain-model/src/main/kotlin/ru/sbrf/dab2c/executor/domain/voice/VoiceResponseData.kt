package ru.sbrf.dab2c.executor.domain.voice

import kotlin.time.Duration

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

/**
 * Age category type.
 */
enum class AgeType {
    /** Not detected. */
    NONE,
    /** Child speaker. */
    CHILD,
    /** Adult speaker. */
    ADULT
}

/**
 * Gender type.
 */
enum class GenderType {
    /** Not detected. */
    NONE,
    /** Male speaker. */
    MALE,
    /** Female speaker. */
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
    val timestamp: Long,
    val stubText: String? = null,
    val inlineData: Map<String, String> = emptyMap(),
    val silencePhrase: Boolean? = null
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

/** Input files data. */
data class InputFilesData(
    val files: List<FileData> = emptyList()
)

/** File reference data. */
data class FileData(
    val id: String,
    val type: String
)

/** Function execution metadata. */
data class PlatformFunctionProcessingData(
    val name: String,
    val timestamp: Long,
)

/** Set of service versions reported to the client. */
data class ServiceInfoData(
    val services: List<ServiceVersion> = emptyList(),
)

/** Service version metadata. */
data class ServiceVersion(
    val serviceName: String,
    val version: String,
    val build: String? = null,
)
