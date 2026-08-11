package ru.sbrf.dab2c.executor.library.context

import kotlinx.coroutines.currentCoroutineContext
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo

/** Returns [Headers] from the current coroutine context or throws. */
suspend fun currentHeaders(): Headers =
    currentCoroutineContext()[HeadersElement]?.headers
        ?: error("HeadersElement not found in coroutine context")

/** Returns [DaSessionInfo] from the current coroutine context or throws. */
suspend fun currentSessionInfo(): DaSessionInfo =
    currentCoroutineContext()[SessionInfoElement]?.sessionInfo
        ?: error("SessionInfoElement not found in coroutine context")

/** Returns the UFS cookie string from the current coroutine context headers. */
suspend fun currentUfsCookie(): String = currentHeaders().ufsCookie
