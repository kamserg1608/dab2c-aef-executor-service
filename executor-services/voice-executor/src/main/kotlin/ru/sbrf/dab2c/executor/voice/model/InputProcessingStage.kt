package ru.sbrf.dab2c.executor.voice.model

/**
 * Input processing stage in the voice session.
 */
enum class InputProcessingStage {
    /** Waiting for initial settings. */
    AWAIT_SETTINGS,
    /** Loading settings from Agent API. */
    LOADING_SETTINGS,
    /** Serving requests normally. */
    SERVING
}
