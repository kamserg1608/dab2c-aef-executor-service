package ru.sbrf.dab2c.executor.clients.efs.adapter.api

/**
 * Client interface for EFS Adapter Parameters API.
 */
interface ParametersClient {

    /** Retrieve a single parameter by name. */
    suspend fun getParameter(name: String): Parameter

    /** Retrieve multiple parameters in a single call. */
    suspend fun getParameters(names: List<String>): Map<String, Parameter>
}
