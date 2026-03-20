package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.voice.model.VoiceSessionFeatureToggles

/** Result of session initialization. */
data class SessionInitResult(val sessionInfo: DaSessionInfo, val featureToggles: VoiceSessionFeatureToggles)

/** Service responsible for initializing a new voice session. */
interface SessionInitService {

    /** Initializes the session and returns session information with feature toggles. */
    suspend fun initialize(): SessionInitResult
}
