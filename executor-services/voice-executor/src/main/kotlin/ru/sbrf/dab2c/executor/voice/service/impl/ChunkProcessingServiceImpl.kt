package ru.sbrf.dab2c.executor.voice.service.impl

import GigaVoiceProtocol.GigaVoice
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.dab2c.executor.voice.util.extensions.extractIf
import ru.sbrf.dab2c.executor.voice.util.extensions.mapIf
import ru.sbrf.dab2c.executor.voice.util.extensions.toResponse

class ChunkProcessingServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannel: Channel<GigaVoice.GigaVoiceRequest>,
    private val settingsService: SettingsService,
    private val functionCallService: FunctionCallService
): ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<GigaVoice.GigaVoiceRequest>
    ): Flow<GigaVoice.GigaVoiceRequest> = merge(
        callbackChannel.receiveAsFlow(),
        processIncomingChunks(requestsChunks)
    )

    private fun processIncomingChunks(requestsChunks: Flow<GigaVoice.GigaVoiceRequest>) = requestsChunks
        .filter { processingState.value.input == SERVING || it.hasSettings() }
        .extractIf({it.hasSettings()}) { settingsService.initSettingsCalculation(it.settings) }

    override fun processResponseChunks(
        responsesChunks: Flow<GigaVoice.GigaVoiceResponse>
    ): Flow<GigaVoice.GigaVoiceResponse> = responsesChunks
        .mapIf({ it.hasFunctionCall() }) { functionCallService.callFunction(it.functionCall)?.toResponse() }

}