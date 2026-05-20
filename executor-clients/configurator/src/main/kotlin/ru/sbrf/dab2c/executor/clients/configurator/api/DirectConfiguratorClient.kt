package ru.sbrf.dab2c.executor.clients.configurator.api

import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse

/**
 * Client interface for EFS Adapter Configurator API.
 */
interface DirectConfiguratorClient {

    /**
     * Get function response.
     */
    suspend fun fetchFunctionRegistry(
        agentName: String,
        modality: String
    ): FunctionListResponse
}
