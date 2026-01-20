package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.ContextData

/**
 * Service for handling context initialization.
 */
interface ContextService {

    /** Processes the initial context chunk. */
    suspend fun processContext(contextData: ContextData)
}
