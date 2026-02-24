package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo

/** Service responsible for initializing a new voice session. */
interface SessionInitService {

    /** Initializes the session and returns the session information. */
    suspend fun initialize(): DaSessionInfo
}
