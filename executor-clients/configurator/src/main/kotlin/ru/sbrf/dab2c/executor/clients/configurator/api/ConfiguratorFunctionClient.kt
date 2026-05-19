package ru.sbrf.dab2c.executor.clients.configurator.api

import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse

/**
 * Client interface for EFS Adapter Configurator API.
 */
interface ConfiguratorFunctionClient {

    /**
     * Get function response.
     */
    suspend fun getFunctionCall(
        agentName: String,
        modality: String
    ): FunctionListResponse
}
