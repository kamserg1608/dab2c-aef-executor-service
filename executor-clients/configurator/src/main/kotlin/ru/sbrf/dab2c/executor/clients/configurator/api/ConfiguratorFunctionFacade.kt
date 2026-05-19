package ru.sbrf.dab2c.executor.clients.configurator.api

/**
 * Unified facade for all EFS Adapter API interactions.
 */
interface ConfiguratorFunctionFacade : ConfiguratorFunctionClient {

    /** EFS Adapter endpoint constants. */
    companion object {
        const val FUNCTION_LIST_ENDPOINT = "/configurator/function/list/v1"
    }
}
