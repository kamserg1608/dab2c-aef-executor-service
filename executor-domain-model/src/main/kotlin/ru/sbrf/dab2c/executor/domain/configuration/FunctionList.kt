package ru.sbrf.dab2c.executor.domain.configuration

/**
 * Response with agent function settings.
 */
data class FunctionList(
    val settings: Settings = Settings()
)

/**
 * Root settings object for function configuration.
 */
data class Settings(
    val gigachat: GigachatSettings = GigachatSettings(),
    val audio: AudioSettings = AudioSettings(),
    val disableInterruption: DisableInterruptionSettings = DisableInterruptionSettings()
)

/**
 * GigaChat-specific settings.
 */
data class GigachatSettings(
    val functions: List<Function> = emptyList(),
    val functionRanker: FunctionRanker = FunctionRanker()
)

/**
 * Function ranking configuration.
 */
data class FunctionRanker(
    val ignoredFunctions: List<String> = emptyList()
)

/**
 * Function metadata available for LLM execution.
 */
data class Function(
    val name: String = "",
    val description: String = "",
    val parameters: String = "",
    val fewShotExamples: List<AnyExample> = emptyList(),
    val returnParameters: String = ""
)

/**
 * Few-shot example for function invocation.
 */
data class AnyExample(
    val request: String = "",
    val params: Params = Params()
)

/**
 * Parameters container for few-shot examples.
 */
data class Params(
    val pairs: List<Pair<String, Any?>> = emptyList()
)

/**
 * Audio-related settings.
 */
data class AudioSettings(
    val output: AudioOutputSettings = AudioOutputSettings()
)

/**
 * Audio output configuration.
 */
data class AudioOutputSettings(
    val stubSounds: StubSounds = StubSounds()
)

/**
 * Stub sound configuration for function execution.
 */
data class StubSounds(
    val triggerFunction: TriggerFunction = TriggerFunction()
)

/**
 * Configuration for trigger sounds during function execution.
 */
data class TriggerFunction(
    val functionNames: List<String> = emptyList(),
    val rules: List<FunctionSoundRule> = emptyList()
)

/**
 * Sound rule for specific functions.
 */
data class FunctionSoundRule(
    val functionNames: List<String> = emptyList(),
    val sounds: List<String> = emptyList()
)

/**
 * Settings for disabling interruption during function execution.
 */
data class DisableInterruptionSettings(
    val functions: List<LockFunctionExecution> = emptyList()
)

/**
 * Function execution lock configuration.
 */
data class LockFunctionExecution(
    val name: String = "",
    val onExecution: Boolean = false,
    val afterResult: Boolean = false
)
