package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession
import ru.sbrf.dab2c.executor.voice.service.api.ChunkProcessingService
import ru.sbrf.dab2c.executor.voice.service.api.ContextService
import ru.sbrf.dab2c.executor.voice.service.api.FunctionCallService
import ru.sbrf.dab2c.executor.voice.service.api.SettingsService

/**
 * Main implementation of ChunkProcessingService with context, settings and function call handling.
 */
class ChunkProcessingServiceImpl(
    private val session: VoiceSession,
    private val contextService: ContextService,
    private val settingsService: SettingsService,
    private val functionCallService: FunctionCallService
) : ChunkProcessingService {

    override fun processRequestChunks(
        requestsChunks: Flow<VoiceRequest>
    ): Flow<VoiceRequest> = merge(
        session.callbackChannels.downstream.receiveAsFlow(),
        routeIncomingChunks(requestsChunks)
            .onCompletion { session.close() }
    ).gateUntilSettingsReceived()

    override fun processResponseChunks(
        responsesChunks: Flow<VoiceResponse>
    ): Flow<VoiceResponse> = merge(
        session.callbackChannels.upstream.receiveAsFlow(),
        routeResponses(responsesChunks)
    ).onCompletion {
        session.terminate()
    }

    private fun routeIncomingChunks(requestsChunks: Flow<VoiceRequest>) = requestsChunks
        .transform { chunk ->
            when {
                chunk is VoiceRequest.Context && session.state.value is ProcessingState.AwaitingContext ->
                    contextService.processContext(chunk.context)
                chunk is VoiceRequest.Settings && session.state.value is ProcessingState.AwaitingSettings ->
                    settingsService.initSettingsCalculation(chunk.settings)
                session.state.value is ProcessingState.Serving ->
                    emit(chunk)
            }
        }

    private fun routeResponses(responsesChunks: Flow<VoiceResponse>) = responsesChunks
        .transform { chunk ->
            if (chunk is VoiceResponse.FunctionCalling) {
                functionCallService.callFunction(chunk.data)
                    ?.let { emit(VoiceResponse.FunctionCalling(it)) }
            } else {
                emit(chunk)
            }
        }

    private fun Flow<VoiceRequest>.gateUntilSettingsReceived(): Flow<VoiceRequest> {
        var settingsReceived = false
        return filter { chunk ->
            if (chunk is VoiceRequest.Settings) settingsReceived = true
            settingsReceived
        }
    }
}
