package ru.sbrf.dab2c.executor.clients.efs.adapter.api

/**
 * Unified facade for all EFS Adapter API interactions.
 */
interface EfsFacade : AuditClient, ConfiguratorClient, ParametersClient, ProfileClient, SdsClient {

    /** EFS Adapter endpoint constants. */
    companion object {
        const val AUDIT_EVENT_ENDPOINT = "/audit/event"
        const val REST_AGENT_ENDPOINT = "/configurator/rest-agent"
        const val SESSION_ENDPOINT = "/configurator/session"
        const val PERSON_INFO_ENDPOINT = "/getPersonInfoByRegionKind"
        const val RETRIEVE_PARAMS_ENDPOINT = "/retrieveParams"
        const val READ_DATA_ENDPOINT = "/session/readData"
        const val WRITE_DATA_ENDPOINT = "/session/writeData"
    }
}
