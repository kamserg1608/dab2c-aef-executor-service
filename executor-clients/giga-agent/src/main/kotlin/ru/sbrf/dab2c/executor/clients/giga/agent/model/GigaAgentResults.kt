package ru.sbrf.dab2c.executor.clients.giga.agent.model

import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Result of settings request to GigaAgent API.
 */
data class SettingsResult(
    val settings: VoiceSettings,
    val performers: FunctionPerformers,
    val analytics: List<AgentAnalytics> = emptyList()
)

/**
 * Result of function call request to GigaAgent API.
 */
data class FunctionCallResult(
    val result: FunctionResultData,
    val analytics: List<AgentAnalytics> = emptyList()
)
