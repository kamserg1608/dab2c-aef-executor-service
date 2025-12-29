package ru.sbrf.dab2c.executor.voice.service.api

import GigaVoiceProtocol.GigaVoice

interface SettingsService {

    suspend fun  initSettingsCalculation(settings: GigaVoice.Settings)

}