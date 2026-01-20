package ru.sbrf.dab2c.executor.clients.efs.adapter.api

import ru.sbrf.dab2c.executor.domain.session.SdsSection

/**
 * Client interface for EFS Adapter SDS (Session Data Storage) API.
 */
interface SdsClient {

    /**
     * Read session data from SDS.
     */
    suspend fun readData(sections: List<SdsSection>, cookie: String): List<SdsSection>

    /**
     * Write session data to SDS.
     */
    suspend fun writeData(sections: List<SdsSection>, cookie: String)
}
