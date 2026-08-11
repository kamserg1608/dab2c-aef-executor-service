package ru.sbrf.dab2c.executor.clients.efs.adapter.api

import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

/**
 * Client interface for EFS Adapter Profile API.
 */
interface ProfileClient {

    /**
     * Gets person info.
     */
    suspend fun getPersonInfo(): DaSessionUserInfo
}
