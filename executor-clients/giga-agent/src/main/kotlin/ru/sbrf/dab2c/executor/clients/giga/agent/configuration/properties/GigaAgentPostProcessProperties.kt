package ru.sbrf.dab2c.executor.clients.giga.agent.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/** Timeout of the `/postprocess` call, overriding the shared giga-agent client timeouts. */
@ConfigurationProperties(prefix = "giga-agent.postprocess")
data class GigaAgentPostProcessProperties(
    val timeout: Long = 60_000
) {
    init {
        require(timeout > 0) { "giga-agent.postprocess.timeout must be positive, but was $timeout" }
    }
}
