package ru.sbrf.dab2c.executor.clients.ivr

import io.grpc.ManagedChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.sbrf.dab2c.executor.clients.ivr.mapper.IvrDomainMapper
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt.IvrServiceCoroutineStub
import ru.sbrf.dab2c.executor.domain.voice.VoiceRequest
import ru.sbrf.dab2c.executor.domain.voice.VoiceResponse

/**
 * Implementation of IvrClient that wraps gRPC communication
 * and converts between domain and proto types.
 */
class IvrClientImpl(
    channel: ManagedChannel
) : IvrClient {

    private val stub = IvrServiceCoroutineStub(channel)

    override fun session(requests: Flow<VoiceRequest>): Flow<VoiceResponse> {
        val protoRequests = requests.map { IvrDomainMapper.toProtoRequest(it) }
        val protoResponses = stub.session(protoRequests)
        return protoResponses.map { IvrDomainMapper.toDomainResponse(it) }
    }
}
