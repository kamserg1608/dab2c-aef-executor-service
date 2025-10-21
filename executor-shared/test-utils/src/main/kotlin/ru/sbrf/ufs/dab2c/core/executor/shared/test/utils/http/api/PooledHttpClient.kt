package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.http.api

import org.apache.http.client.HttpClient

/**
 * Represents an HTTP client that supports pooled connections.
 */
interface PooledHttpClient : HttpClient {

    /**
     * Releases all active connections held by HTTP client.
     */
    fun releaseConnections()
}
