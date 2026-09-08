package ru.sbrf.dab2c.executor.clients.giga.agent.model

import ru.sbrf.dab2c.executor.clients.gigavoice.proto.FunctionResult
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers

/**
 * Result of settings request to GigaAgent API.
 */
data class SettingsResult(
    val settings: Settings,
    val performers: FunctionPerformers,
    val analytics: List<AgentAnalytics> = emptyList(),
    val context: DialogContext? = null
)

/**
 * Result of function call request to GigaAgent API.
 */
data class FunctionCallResult(
    val result: FunctionResult,
    val analytics: List<AgentAnalytics> = emptyList(),
    val context: DialogContext? = null
)

/**
 * Result of post-processing request to GigaAgent API.
 */
data class PostProcessResult(
    val analytics: List<AgentAnalytics> = emptyList()
)
