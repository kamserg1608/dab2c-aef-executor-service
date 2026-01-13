package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

interface SettingsService {

    suspend fun initSettingsCalculation(settings: VoiceSettings)
}
