package ru.sbrf.dab2c.executor.voice.service.api

/**
 * Service for initializing session data before processing begins.
 */
interface SessionInitService {

    /** Initializes session data (e.g., fetches DaSessionInfo) before message processing. */
    suspend fun initialize()
}
