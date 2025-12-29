package ru.sbrf.dab2c.executor.client.configurator.api

import ru.sbrf.da.configurator.client.acl.AgentConfig

interface AgentConfigurationProvider {

    suspend fun loadRestAgent(): Map<String, AgentConfig>

}
