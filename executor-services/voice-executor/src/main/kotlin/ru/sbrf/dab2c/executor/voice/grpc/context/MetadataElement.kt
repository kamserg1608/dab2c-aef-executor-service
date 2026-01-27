package ru.sbrf.dab2c.executor.voice.grpc.context

import ru.sbrf.dab2c.executor.voice.model.RequestMetadata
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element for propagating request metadata across coroutines.
 */
class MetadataElement(val metadata: RequestMetadata) : CoroutineContext.Element {
    override val key get() = Key

    /**
     * Coroutine context key for [MetadataElement].
     */
    companion object Key : CoroutineContext.Key<MetadataElement>
}
