package ru.sbrf.dab2c.executor.clients.efs.adapter.api

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.FunctionConfig
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon

/**
 * Client interface for EFS Adapter Configurator API.
 */
interface ConfiguratorClient {

    /**
     * Get REST agent configuration by agent name.
     */
    suspend fun getRestAgentConfig(agentName: String): AgentConfiguration

    /**
     * Get DaSessionCommon.
     */
    suspend fun getDaSessionCommon(): DaSessionCommon
    /**
     * Get function configuration by agent and function name.
     */
    suspend fun getFunction(
        agentName: String,
        functionName: String
    ): Map<String, FunctionConfig>
}
