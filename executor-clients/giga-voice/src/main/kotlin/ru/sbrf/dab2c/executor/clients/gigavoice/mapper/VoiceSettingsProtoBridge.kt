package ru.sbrf.dab2c.executor.clients.gigavoice.mapper

import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings

/** Converts proto Settings to domain VoiceSettings. */
fun Settings.toDomain(): VoiceSettings = IvrDomainMapper.toDomainSettings(this)

/** Converts domain VoiceSettings to proto Settings. */
fun VoiceSettings.toProto(): Settings = GigaVoiceDomainMapper.toProtoSettings(this)
