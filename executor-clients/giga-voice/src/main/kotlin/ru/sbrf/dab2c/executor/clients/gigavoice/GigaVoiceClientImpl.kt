package ru.sbrf.dab2c.executor.clients.gigavoice

import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.sbrf.dab2c.executor.clients.gigavoice.mapper.GigaVoiceDomainMapper
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * Implementation of GigaVoiceClient that wraps gRPC communication
 * and converts between domain and proto types.
 */
class GigaVoiceClientImpl(
    channel: ManagedChannel
) : GigaVoiceClient {

    private val stub = GigaVoiceServiceCoroutineStub(channel)

    override fun session(requests: Flow<VoiceRequest>): Flow<VoiceResponse> {
        val protoRequests = requests.map { GigaVoiceDomainMapper.toProtoRequest(it) }
        val protoResponses = stub.gigaVoice(protoRequests)
        return protoResponses.map { GigaVoiceDomainMapper.toDomainResponse(it) }
    }
}
