package ru.sbrf.ufs.dab2c.core.voice

import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.sbrf.ufs.dab2c.core.ApplicationEntryPoint

@OptIn(ExperimentalCoroutinesApi::class)
@SpringBootTest(
    classes = [ApplicationEntryPoint::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["STUB", "stubMode", "test"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseGigaVoiceIntegrationTest {

    @Value("\${grpc.server.port}")
    private var grpcServerPort: Int = 0

    @Autowired
    protected lateinit var mockDownstreamService: MockGigaVoiceDownstreamService

    private lateinit var clientChannel: ManagedChannel
    protected lateinit var clientStub: GigaVoiceServiceCoroutineStub

    @BeforeEach
    fun resetState() {
        mockDownstreamService.reset()
    }

    @BeforeAll
    fun setup() {
        clientChannel = ManagedChannelBuilder
            .forAddress("localhost", grpcServerPort)
            .usePlaintext()
            .build()

        clientStub = GigaVoiceServiceCoroutineStub(clientChannel)
    }

    @AfterAll
    fun teardown() {
        clientChannel.shutdown()
    }
}
