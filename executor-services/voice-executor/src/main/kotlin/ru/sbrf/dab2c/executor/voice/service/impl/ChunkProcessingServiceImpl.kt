package ru.sbrf.dab2c.executor.voice.service.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.transform
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.voice.mapper.toDomain
import ru.sbrf.dab2c.executor.voice.mapper.toProto
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
        requestsChunks: Flow<GigaVoiceRequest>
    ): Flow<GigaVoiceRequest> = merge(
        session.callbackChannels.downstream.receiveAsFlow(),
        routeIncomingChunks(requestsChunks)
            .onCompletion { session.close() }
    ).gateUntilSettingsReceived()

    override fun processResponseChunks(
        responsesChunks: Flow<GigaVoiceResponse>
    ): Flow<GigaVoiceResponse> = merge(
        session.callbackChannels.upstream.receiveAsFlow(),
        routeResponses(responsesChunks)
    ).onCompletion {
        session.terminate()
    }

    private fun routeIncomingChunks(requestsChunks: Flow<GigaVoiceRequest>) = requestsChunks
        .transform { chunk ->
            when {
                chunk.requestCase == GigaVoiceRequest.RequestCase.CONTEXT &&
                    session.state.value is ProcessingState.AwaitingContext ->
                    contextService.processContext(chunk.context.toDomain())
                chunk.requestCase == GigaVoiceRequest.RequestCase.SETTINGS &&
                    session.state.value is ProcessingState.AwaitingSettings ->
                    settingsService.initSettingsCalculation(chunk.settings.toDomain())
                session.state.value is ProcessingState.Serving ->
                    emit(chunk)
            }
        }

    private fun routeResponses(responsesChunks: Flow<GigaVoiceResponse>) = responsesChunks
        .transform { chunk ->
            if (chunk.responseCase == GigaVoiceResponse.ResponseCase.FUNCTION_CALL) {
                functionCallService.callFunction(chunk.functionCall.toDomain())
                    ?.let { emit(gigaVoiceResponse { functionCall = it.toProto() }) }
            } else {
                emit(chunk)
            }
        }

    private fun Flow<GigaVoiceRequest>.gateUntilSettingsReceived(): Flow<GigaVoiceRequest> {
        var settingsReceived = false
        return filter { chunk ->
            if (chunk.requestCase == GigaVoiceRequest.RequestCase.SETTINGS) settingsReceived = true
            settingsReceived
        }
    }
}
