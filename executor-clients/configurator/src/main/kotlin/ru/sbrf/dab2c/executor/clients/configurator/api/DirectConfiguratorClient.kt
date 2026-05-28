package ru.sbrf.dab2c.executor.clients.configurator.api

import ru.sbrf.dab2c.executor.domain.configuration.FunctionListResponse

/**
 * Client interface for Configurator API.
 */
interface DirectConfiguratorClient {

    /**
     * Get function response.
     */
    suspend fun fetchFunctionRegistry(
        agentName: String,
        modality: String
    ): FunctionListResponse

    /**
     * Configurator endpoint constants.
     */
    companion object {
        const val FUNCTION_LIST_ENDPOINT = "/function/list/v1"
    }
}
