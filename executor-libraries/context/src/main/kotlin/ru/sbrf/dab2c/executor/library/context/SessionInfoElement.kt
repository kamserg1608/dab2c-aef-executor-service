package ru.sbrf.dab2c.executor.library.context

import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element carrying [DaSessionInfo].
 */
class SessionInfoElement(val sessionInfo: DaSessionInfo) : CoroutineContext.Element {
    override val key get() = Key

    /** Coroutine context key. */
    companion object Key : CoroutineContext.Key<SessionInfoElement>
}
