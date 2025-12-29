package ru.sbrf.dab2c.executor.client.configurator.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import ru.sbrf.da.configurator.client.acl.AgentConfig
import ru.sbrf.dab2c.executor.client.configurator.api.AgentConfigurationProvider

@Service
class AgentConfigurationProviderImpl : AgentConfigurationProvider {

    //TODO Интеграция через сайдкар
    override suspend fun loadRestAgent(): Map<String, AgentConfig> = withContext(Dispatchers.IO) {
        emptyMap()
    }

}
