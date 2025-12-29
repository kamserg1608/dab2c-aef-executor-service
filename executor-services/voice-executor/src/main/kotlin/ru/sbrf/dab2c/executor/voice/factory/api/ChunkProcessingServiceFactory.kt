package ru.sbrf.dab2c.executor.voice.factory.api

import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService

interface ChunkProcessingServiceFactory {

    fun create(): ChunkProcessingService

}