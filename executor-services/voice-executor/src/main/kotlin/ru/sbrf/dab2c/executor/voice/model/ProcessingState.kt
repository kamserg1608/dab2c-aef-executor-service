package ru.sbrf.dab2c.executor.voice.model

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers

/**
 * Current processing state of the voice session.
 */
data class ProcessingState(
    val input: InputProcessingStage = InputProcessingStage.AWAIT_CONTEXT,
    val output: OutputProcessingStage = OutputProcessingStage.SERVING,
    val functionRegistry: FunctionPerformers = FunctionPerformers(),
    val agentConfiguration: AgentConfiguration? = null,
    val sessionConfiguration: SessionConfiguration? = null,
    val conversationId: String? = null,
    val daSessionInfo: DaSessionInfo? = null,
    val contextData: ContextData? = null
)
