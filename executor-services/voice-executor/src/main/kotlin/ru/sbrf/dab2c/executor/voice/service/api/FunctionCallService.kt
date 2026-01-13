package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData

interface FunctionCallService {

    /**
     * Executes function calls.
     */
    suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData?
}
