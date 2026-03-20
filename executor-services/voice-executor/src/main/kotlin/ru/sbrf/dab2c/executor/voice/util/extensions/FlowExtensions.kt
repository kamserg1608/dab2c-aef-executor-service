package ru.sbrf.dab2c.executor.voice.util.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureTogglesElement
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitResult
import kotlin.coroutines.CoroutineContext

/** Maps elements matching predicate, pass through others unchanged. */
fun <T> Flow<T>.mapIf(predicate: (T) -> Boolean, transform: suspend (T) -> T?): Flow<T> =
    mapNotNull { if (predicate(it)) transform(it) else it }

/** Extracts and processes elements matching predicate, emits others unchanged. */
fun <T> Flow<T>.extractIf(
    predicate: suspend (T) -> Boolean,
    action: suspend (T) -> Unit
): Flow<T> = transform { if (predicate(it)) { action(it) } else { emit(it) } }

/** Wraps a flow with session context. */
fun <T> Flow<T>.withSessionContext(
    baseContext: CoroutineContext,
    init: suspend () -> SessionInitResult
): Flow<T> = flow {
    val result = withContext(baseContext) { init() }
    val fullContext = baseContext +
        SessionInfoElement(result.sessionInfo) +
        VoiceSessionFeatureTogglesElement(result.featureToggles)
    emitAll(this@withSessionContext.flowOn(fullContext))
}
