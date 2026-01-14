package ru.sbrf.dab2c.executor.voice.factory.api

import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

/**
 * Factory for creating ChunkProcessingService instances.
 */
interface ChunkProcessingServiceFactory {

    /**
     * Creates a new ChunkProcessingService instance.
     */
    fun create(): ChunkProcessingService
}
