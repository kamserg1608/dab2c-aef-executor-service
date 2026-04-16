package ru.sbrf.dab2c.executor.domain.voice

import kotlin.time.Duration

/**
 * Main voice session settings.
 */
data class VoiceSettings(
    val voiceCallId: String,
    val audio: AudioSettings,
    val gigachat: GigaChatSettings? = null,
    val context: InitialContext? = null,
    val disableVad: Boolean = false,
    val enableTranscribeInput: Boolean = false,
    val flags: List<String> = emptyList(),
    val outputModalities: OutputModalities = OutputModalities.UNSPECIFIED,
    val mode: VoiceMode = VoiceMode.UNSPECIFIED,
    val firstSpeaker: FirstSpeaker? = null,
    val enableDenoiser: Boolean = false,
    val enablePrefetch: Boolean = false,
    val enablePersonIdentity: Boolean = false,
    val enableWhisper: Boolean = false,
    val enableEmotion: Boolean = false,
    val enableTranscribeSilencePhrases: Boolean = false,
    val disableInterruption: DisableInterruption? = null,
)

/**
 * Audio input/output settings.
 */
data class AudioSettings(
    val input: AudioInputSettings? = null,
    val output: AudioOutputSettings? = null
)

/**
 * Audio input (ASR) settings.
 */
data class AudioInputSettings(
    val model: String? = null,
    val audioEncoding: AudioEncoding = AudioEncoding.UNSPECIFIED,
    val sampleRate: Int? = null,
    val silencePhrases: List<String> = emptyList(),
    val silencePhrasesTimeout: Duration? = null,
    val silenceTimeout: Duration? = null,
    val stopPhrases: List<String> = emptyList(),
    val ignorePhrases: List<String> = emptyList()
)

/**
 * Audio output (TTS) settings.
 */
data class AudioOutputSettings(
    val voice: String? = null,
    val audioEncoding: AudioEncoding = AudioEncoding.UNSPECIFIED,
    val stubSounds: StubSounds? = null,
    val speed: Speed = Speed.SPEED_UNSPECIFIED,
)

/**
 * GigaChat LLM settings.
 */
data class GigaChatSettings(
    val model: String? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val repetitionPenalty: Float? = null,
    val updateInterval: Float? = null,
    val profanityCheck: Boolean? = null,
    val filtersSettings: Map<String, FilterSettings> = emptyMap(),
    val functions: List<FunctionDefinition> = emptyList(),
    val functionRegistry: FunctionRegistry? = null,
    val filterStubPhrases: List<String> = emptyList(),
    val currentTime: Int? = null,
    val functionRanker: FunctionRanker? = null,
    val preset: String? = null,
)

/**
 * Content filter settings.
 */
data class FilterSettings(
    val requestContent: RequestContentSettings? = null,
    val responseContent: ResponseContentSettings? = null
)

/**
 * Request content filter settings.
 */
data class RequestContentSettings(
    val neuro: Boolean? = null,
    val blacklist: Boolean? = null,
    val whitelist: Boolean? = null
)

/**
 * Response content filter settings.
 */
data class ResponseContentSettings(
    val blacklist: Boolean? = null
)

/**
 * Function definition for GigaChat.
 */
data class FunctionDefinition(
    val name: String,
    val description: String? = null,
    val parameters: String? = null,
    val fewShotExamples: List<FunctionExample> = emptyList(),
    val returnParameters: String? = null
)

/**
 * Function few-shot example.
 */
data class FunctionExample(
    val request: String,
    val params: List<Pair<String, String>> = emptyList()
)

/**
 * Function registry configuration.
 */
data class FunctionRegistry(
    val profile: String? = null,
    val labels: List<String> = emptyList(),
    val abFlags: String? = null
)

/**
 * Initial conversation context.
 */
data class InitialContext(
    val messages: List<Message> = emptyList()
)

/**
 * Conversation message.
 */
data class Message(
    val role: String,
    val content: String,
    val functionCall: FunctionCall? = null,
    val functionName: String? = null,
    val functionsStateId: String? = null,
    val attachments: List<String> = emptyList(),
    val inlineData: Map<String, String> = emptyMap(),
    val functions: List<FunctionDefinition> = emptyList()
)

/**
 * First speaker configuration.
 */
data class FirstSpeaker(
    val type: String? = null,
    val lockFirstIn: Boolean? = null
)

/** Stub sounds configuration for function call placeholders. */
data class StubSounds(
    val triggerGeneration: TriggerGeneration? = null,
    val triggerFunction: TriggerFunction? = null,
    val sounds: List<String> = emptyList()
)

/** Trigger settings for generation-based stub sounds. */
data class TriggerGeneration(
    val timeout: Duration? = null,
    val enable: Boolean = false
)

/** Trigger settings for function-call-based stub sounds. */
data class TriggerFunction(
    val enable: Boolean = false,
    val mode: TriggerFunctionMode = TriggerFunctionMode.UNSPECIFIED,
    val functionNames: List<String> = emptyList(),
    val rules: List<FunctionSoundRule> = emptyList(),
)

/** Function trigger filtering mode. */
enum class TriggerFunctionMode {
    UNSPECIFIED,
    WHITELIST,
    BLACKLIST
}

/** Function ranking configuration. */
data class FunctionRanker(
    val enabled: Boolean? = null,
    val topN: Int? = null,
    val embedderModel: String? = null,
    val ignoredFunctions: List<String> = emptyList(),
)

/** Настройки игнорирования голосового ввода. */
data class DisableInterruption(
    val functions: List<LockFunctionExecution> = emptyList(),
)

/** Настройки блокировки перебивания по функциям. */
data class LockFunctionExecution(
    val name: String,
    val onExecution: Boolean? = null,
    val afterResult: Boolean? = null,
)

/** Правило для сопоставления функций и звуков. */
data class FunctionSoundRule(
    val functionNames: List<String> = emptyList(),
    val sounds: List<String> = emptyList(),
)
