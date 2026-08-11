package ru.sbrf.dab2c.executor.voice.service.api

import kotlinx.coroutines.flow.Flow
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles

/** Result of session initialization. */
data class SessionInitResult(val sessionInfo: DaSessionInfo, val featureToggles: VoiceSessionFeatureToggles)

/** Service responsible for initializing a new voice session. */
interface SessionInitService {

    /** Initializes the session and returns session information with feature toggles. */
    suspend fun initialize(): SessionInitResult

    /** Wraps a flow with initialized session context (headers, session info, MDC with masking). */
    fun <T> Flow<T>.withSessionContext(headers: Headers): Flow<T>
}
