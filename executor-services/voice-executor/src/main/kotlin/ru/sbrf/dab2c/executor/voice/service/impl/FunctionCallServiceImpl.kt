package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<VoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient
) : FunctionCallService {

    override suspend fun callFunction(functionCalling: FunctionCallingData): FunctionCallingData? {
        // TODO: Needs non-stub implementation
        val functionCall = functionCalling.functionCall

        if (functionCall.name.contains("avg")) {
            // TODO: Call gigaVoiceAgentClient.executeFunctionCall()
            callBackChannel.send(
                VoiceRequest.FunctionResult(
                    FunctionResultData(
                        content = "NOOP",
                        functionName = functionCall.name
                    )
                )
            )
            return null
        }

        return functionCalling
    }
}
