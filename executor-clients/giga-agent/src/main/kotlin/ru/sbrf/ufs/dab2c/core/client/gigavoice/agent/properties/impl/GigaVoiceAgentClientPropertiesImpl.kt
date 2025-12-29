package ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.properties.impl

import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.library.sup.api.ExecutorConfigService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.properties.api.GigaVoiceAgentClientProperties

@Service
class GigaVoiceAgentClientPropertiesImpl(
    private val executorConfigService: ExecutorConfigService
): GigaVoiceAgentClientProperties {

    override val baseUrl: String
        get() = "http://localhost:8080"

    override val connectionTimeout: Long
        get() = 120_000

    override val requestTimeout: Long
        get() = 120_000
}