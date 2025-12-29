package ru.sbrf.dab2c.executor.voice.service.api

import GigaVoiceProtocol.GigaVoice

interface FunctionCallService {

    /**
     * Executes functionCalls
     */
    suspend fun callFunction(functionCalling: GigaVoice.FunctionCalling): GigaVoice.FunctionCalling?

}