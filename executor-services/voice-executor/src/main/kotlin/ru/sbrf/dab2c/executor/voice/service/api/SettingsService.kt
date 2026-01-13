package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/**
 * Service for initializing voice session settings.
 */
interface SettingsService {

    /** Initializes settings calculation from Agent API. */
    suspend fun initSettingsCalculation(settings: VoiceSettings)
}
