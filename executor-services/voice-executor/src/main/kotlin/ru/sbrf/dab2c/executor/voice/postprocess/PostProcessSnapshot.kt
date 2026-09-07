package ru.sbrf.dab2c.executor.voice.postprocess

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.voice.DialogContext

/** Everything post-processing needs from a finished session, detached from the session itself. */
data class PostProcessSnapshot(
    val conversationId: String,
    val agentConfiguration: AgentConfiguration,
    val contextData: DialogContext,
    val assistantMessageId: String
)
