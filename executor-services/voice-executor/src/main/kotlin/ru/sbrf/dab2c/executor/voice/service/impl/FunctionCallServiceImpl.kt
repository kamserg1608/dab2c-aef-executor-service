package ru.sbrf.dab2c.executor.voice.service.impl

import GigaVoiceProtocol.GigaVoice
import GigaVoiceProtocol.GigaVoice.FunctionResult
import GigaVoiceProtocol.GigaVoice.GigaVoiceRequest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.ufs.dab2c.core.client.gigavoice.agent.api.GigaVoiceAgentClient

class FunctionCallServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callBackChannel: Channel<GigaVoiceRequest>,
    private val gigaVoiceAgentClient: GigaVoiceAgentClient
): FunctionCallService {

    override suspend fun callFunction(functionCalling: GigaVoice.FunctionCalling): GigaVoice.FunctionCalling? {
        //TODO Needs not stub implementation
        val functionCall = functionCalling.functionCall

        if (functionCall.name.contains("avg")) {
            //TODO Call avg
            callBackChannel.send(
                GigaVoiceRequest.newBuilder()
                    .setFunctionResult(
                        FunctionResult.newBuilder()
                            .setFunctionName(functionCall.name)
                            .setContent("NOOP")
                            .build()
                    )
                    .build()
            )
            return null
        }

        return functionCalling
    }
}