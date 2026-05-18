package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Context
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
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
        val contextData: Context
    ) : ProcessingState()

    /**
     * Fully initialized and serving requests.
     */
    data class Serving(
        val contextData: Context,
        val agentConfiguration: AgentConfiguration,
        val conversationId: String,
        val functionRegistry: FunctionPerformers
    ) : ProcessingState()
}
