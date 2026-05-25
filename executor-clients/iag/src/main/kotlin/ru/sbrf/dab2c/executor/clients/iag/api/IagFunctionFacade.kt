package ru.sbrf.dab2c.executor.clients.iag.api

/**
 * Unified facade for all EFS Adapter API interactions.
 */
interface IagFunctionFacade : IagFunctionClient {

    /** EFS Adapter endpoint constants. */
    companion object {
        const val FUNCTION_CALL_ENDPOINT = "/function/call"
    }
}
