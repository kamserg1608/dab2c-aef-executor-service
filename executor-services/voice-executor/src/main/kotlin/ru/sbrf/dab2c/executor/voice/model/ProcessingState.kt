package ru.sbrf.dab2c.executor.voice.model

/**
 * Current processing state of the voice session.
 */
data class ProcessingState(
    val input: InputProcessingStage = InputProcessingStage.AWAIT_SETTINGS,
    val output: OutputProcessingStage = OutputProcessingStage.SERVING,
    val functionRegistry: Map<String, String> = emptyMap()
)
