package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.model.CallbackChannels
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.ContextService
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService
import ru.sbrf.dab2c.executor.voice.util.extensions.extractIf
import ru.sbrf.dab2c.executor.voice.util.extensions.mapIf

/**
 * Main implementation of ChunkProcessingService with context, settings and function call handling.
 */
class ChunkProcessingServiceImpl(
    private val processingState: MutableStateFlow<ProcessingState>,
    private val callbackChannels: CallbackChannels,
    private val contextService: ContextService,
    private val settingsService: SettingsService,
    private val functionCallService: FunctionCallService
) : ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> = merge(
        callbackChannels.downstream.receiveAsFlow(),
        processIncomingChunks(requestsChunks)
    )
        .onEach {
            if (it is VoiceRequest.Settings && processingState.value is ProcessingState.Serving) {
                processingState.value = (processingState.value as ProcessingState.Serving).copy(settingsSent = true)
            }
        }

    private fun processIncomingChunks(requestsChunks: Flow<VoiceRequest>) = requestsChunks
        .filter { isChunkAllowed(it) }
        .extractIf({ it is VoiceRequest.Context }) {
            contextService.processContext((it as VoiceRequest.Context).context)
        }
        .extractIf({ it is VoiceRequest.Settings }) {
            settingsService.initSettingsCalculation((it as VoiceRequest.Settings).settings)
        }

    private fun isChunkAllowed(chunk: VoiceRequest): Boolean {
        val state = processingState.value
        return when (chunk) {
            is VoiceRequest.Context -> state is ProcessingState.AwaitingContext
            is VoiceRequest.Settings -> state is ProcessingState.AwaitingSettings
            else -> state is ProcessingState.Serving && state.settingsSent
        }
    }

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> = merge(
        callbackChannels.upstream.receiveAsFlow(),
        responsesChunks
            .mapIf({ it is VoiceResponse.FunctionCalling }) {
                functionCallService.callFunction((it as VoiceResponse.FunctionCalling).data)
                    ?.let { data -> VoiceResponse.FunctionCalling(data) }
            }
    )
}
