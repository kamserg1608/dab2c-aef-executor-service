package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.AWAIT_CONTEXT
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.AWAIT_SETTINGS
import ru.sbrf.dab2c.executor.voice.model.InputProcessingStage.SERVING
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
    private val callbackChannel: Channel<VoiceRequest>,
    private val contextService: ContextService,
    private val settingsService: SettingsService,
    private val functionCallService: FunctionCallService
) : ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> = merge(
        callbackChannel.receiveAsFlow(),
        processIncomingChunks(requestsChunks)
    )

    private fun processIncomingChunks(requestsChunks: Flow<VoiceRequest>) = requestsChunks
        .filter { isChunkAllowed(it) }
        .extractIf({ it is VoiceRequest.Context }) {
            contextService.processContext((it as VoiceRequest.Context).context)
        }
        .extractIf({ it is VoiceRequest.Settings }) {
            settingsService.initSettingsCalculation((it as VoiceRequest.Settings).settings)
        }

    private fun isChunkAllowed(chunk: VoiceRequest): Boolean {
        val stage = processingState.value.input
        return when (chunk) {
            is VoiceRequest.Context -> stage == AWAIT_CONTEXT
            is VoiceRequest.Settings -> stage == AWAIT_SETTINGS
            else -> stage == SERVING
        }
    }

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> = responsesChunks
        .mapIf({ it is VoiceResponse.FunctionCalling }) {
            functionCallService.callFunction((it as VoiceResponse.FunctionCalling).data)
                ?.let { data -> VoiceResponse.FunctionCalling(data) }
        }
}
