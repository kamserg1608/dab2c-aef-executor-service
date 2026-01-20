package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers

/**
 * Sealed class representing the processing state of a voice session.
 * Each subclass represents a distinct stage with its available data.
 */
sealed class ProcessingState {

    /**
     * Initial state awaiting context data from IVR.
     */
    data object AwaitingContext : ProcessingState()

    /**
     * Received context, awaiting settings from IVR.
     */
    data class AwaitingSettings(
        val contextData: ContextData
    ) : ProcessingState()

    /**
     * Loading settings from external APIs.
     */
    data class LoadingSettings(
        val contextData: ContextData
    ) : ProcessingState()

    /**
     * Fully initialized and serving requests.
     */
    data class Serving(
        val contextData: ContextData,
        val agentConfiguration: AgentConfiguration,
        val sessionConfiguration: SessionConfiguration,
        val conversationId: String,
        val daSessionInfo: DaSessionInfo,
        val functionRegistry: FunctionPerformers
    ) : ProcessingState()
}
