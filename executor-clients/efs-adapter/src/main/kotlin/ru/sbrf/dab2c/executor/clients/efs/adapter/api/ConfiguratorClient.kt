package ru.sbrf.dab2c.executor.clients.efs.adapter.api

import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration

/**
 * Client interface for EFS Adapter Configurator API.
 */
interface ConfiguratorClient {

    /**
     * Get REST agent configuration by agent name.
     */
    suspend fun getRestAgentConfig(agentName: String, cookie: String): AgentConfiguration

    /**
     * Get session configuration containing channel and platform info.
     */
    suspend fun getSessionConfig(cookie: String): SessionConfiguration
}
