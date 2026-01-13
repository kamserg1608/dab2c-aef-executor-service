package ru.sbrf.dab2c.executor.voice.util.extensions

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.transform

/** Maps elements matching predicate, pass through others unchanged. */
fun <T> Flow<T>.mapIf(predicate: (T) -> Boolean, transform: suspend (T) -> T?): Flow<T> =
    mapNotNull { if (predicate(it)) transform(it) else it }

/** Extracts and processes elements matching predicate, emits others unchanged. */
fun <T> Flow<T>.extractIf(
    predicate: suspend (T) -> Boolean,
    action: suspend (T) -> Unit
): Flow<T> = transform { if (predicate(it)) { action(it) } else { emit(it) } }
