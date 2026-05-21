package ru.sbrf.dab2c.executor.clients.configurator.api

/**
 * Unified facade for all EFS Adapter API interactions.
 */
interface DirectConfiguratorFacade : DirectConfiguratorClient {

    /** EFS Adapter endpoint constants. */
    companion object {
        const val FUNCTION_LIST_ENDPOINT = "/function/list/v1"
    }
}
