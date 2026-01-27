package ru.sbrf.dab2c.executor.voice.util.extensions

import kotlinx.coroutines.currentCoroutineContext
import ru.sbrf.dab2c.executor.voice.grpc.context.MetadataElement
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

/**
 * Retrieves the current request metadata from the coroutine context.
 */
suspend fun currentRequestMetadata(): RequestMetadata = currentCoroutineContext()[MetadataElement]!!.metadata
