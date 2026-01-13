package ru.sbrf.dab2c.executor.voice.service.api

import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData

/**
 * Service for executing function calls via Agent API.
 */
interface FunctionCallService {

    /** Executes function call and returns updated data. */
    suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData?
}
