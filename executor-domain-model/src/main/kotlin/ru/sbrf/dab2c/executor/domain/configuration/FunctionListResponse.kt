package ru.sbrf.dab2c.executor.domain.configuration

/**
 * Response with agent function settings.
 */
data class FunctionListResponse(
    val settings: Settings
)

/**
 * Root settings object for function configuration.
 */
data class Settings(
    val gigachat: GigachatSettings,
    val audio: AudioSettings,
    val disableInterruption: DisableInterruptionSettings
)

/**
 * GigaChat-specific settings.
 */
data class GigachatSettings(
    val functions: List<Function>,
    val functionRanker: FunctionRanker
)

/**
 * Function ranking configuration.
 */
data class FunctionRanker(
    val ignoredFunctions: List<String>
)

/**
 * Function metadata available for LLM execution.
 */
data class Function(
    val name: String,
    val description: String,
    val parameters: String,
    val fewShotExamples: List<AnyExample> = emptyList(),
    val returnParameters: String
)

/**
 * Few-shot example for function invocation.
 */
data class AnyExample(
    val request: String,
    val params: Params
)

/**
 * Parameters container for few-shot examples.
 */
data class Params(
    val pairs: List<Pair<String, Any?>>
)

/**
 * Audio-related settings.
 */
data class AudioSettings(
    val output: AudioOutputSettings
)

/**
 * Audio output configuration.
 */
data class AudioOutputSettings(
    val stubSounds: StubSounds
)

/**
 * Stub sound configuration for function execution.
 */
data class StubSounds(
    val triggerFunction: TriggerFunction
)

/**
 * Configuration for trigger sounds during function execution.
 */
data class TriggerFunction(
    val functionNames: List<String>,
    val rules: List<FunctionSoundRule>
)

/**
 * Sound rule for specific functions.
 */
data class FunctionSoundRule(
    val functionNames: List<String>,
    val sounds: List<String>
)

/**
 * Settings for disabling interruption during function execution.
 */
data class DisableInterruptionSettings(
    val functions: List<LockFunctionExecution>
)

/**
 * Function execution lock configuration.
 */
data class LockFunctionExecution(
    val name: String,
    val onExecution: Boolean,
    val afterResult: Boolean
)
