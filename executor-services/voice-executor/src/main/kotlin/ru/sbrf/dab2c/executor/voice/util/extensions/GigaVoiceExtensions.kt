package ru.sbrf.dab2c.executor.voice.util.extensions

import GigaVoiceProtocol.GigaVoice

fun GigaVoice.FunctionCalling.toResponse() = GigaVoice.GigaVoiceResponse
    .newBuilder()
    .setFunctionCall(this)
    .build()

fun GigaVoice.Settings.toRequest() = GigaVoice.GigaVoiceRequest
    .newBuilder()
    .setSettings(this)
    .build()