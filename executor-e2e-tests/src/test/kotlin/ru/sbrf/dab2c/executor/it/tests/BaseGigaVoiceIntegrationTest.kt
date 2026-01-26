package ru.sbrf.dab2c.executor.it.tests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.github.tomakehurst.wiremock.WireMockServer
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.serialization.jackson.jackson
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import ru.sbrf.dab2c.executor.application.ApplicationEntryPoint
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt.IvrServiceCoroutineStub
import ru.sbrf.dab2c.executor.it.mock.MockGigaVoiceService
import ru.sbrf.dab2c.executor.it.support.grpc.MetadataInterceptor
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubSdsSessionReadData

/**
 * Base class for integration tests.
 * Provides Spring context, gRPC client, HTTP client, and WireMock configuration.
 */
@SpringBootTest(
    classes = [ApplicationEntryPoint::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["STUB", "stubMode", "test"])
@EnableWireMock(
    ConfigureWireMock(name = "gigaVoiceAgent", baseUrlProperties = ["giga.voice.agent.client.baseUrl"]),
    ConfigureWireMock(name = "efsAdapter", baseUrlProperties = ["efs.adapter.baseUrl"])
)
@EmbeddedKafka(
    partitions = 1,
    topics = ["dab2c-core-dialogs", "dab2c-agents"]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseGigaVoiceIntegrationTest {

    @InjectWireMock("gigaVoiceAgent")
    protected lateinit var gigaVoiceAgentMock: WireMockServer

    @InjectWireMock("efsAdapter")
    protected lateinit var efsAdapterMock: WireMockServer

    @Value("\${grpc.server.port}")
    private var grpcServerPort: Int = 0

    @LocalServerPort
    var port: Int = 0

    @Value("\${spring.webflux.base-path:}")
    protected lateinit var basePath: String

    @Autowired
    protected lateinit var mockGigaVoiceService: MockGigaVoiceService

    @Autowired
    protected lateinit var embeddedKafkaBroker: EmbeddedKafkaBroker

    private lateinit var clientChannel: ManagedChannel
    protected lateinit var clientStub: IvrServiceCoroutineStub

    protected val httpClient: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson {
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                }
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }
            defaultRequest {
                url.protocol = URLProtocol.HTTP
                url.host = "localhost"
                url.port = this@BaseGigaVoiceIntegrationTest.port
            }
        }
    }

    @BeforeEach
    fun resetState() {
        mockGigaVoiceService.reset()
        gigaVoiceAgentMock.resetAll()
        efsAdapterMock.resetAll()
        efsAdapterMock.stubSdsSessionReadData()
    }

    @BeforeAll
    fun setup() {
        clientChannel = ManagedChannelBuilder
            .forAddress("localhost", grpcServerPort)
            .usePlaintext()
            .build()

        clientStub = IvrServiceCoroutineStub(clientChannel)
    }

    @AfterAll
    fun teardown() {
        clientChannel.shutdown()
    }

    /** Creates a stub configured for proxy mode (pass-through). */
    protected fun proxyStub(): IvrServiceCoroutineStub =
        clientStub.withInterceptors(
            MetadataInterceptor(
                mapOf(
                    "proxy" to "true",
                    "x-channel" to "MOB_BANK",
                    "x-token" to "proxy-token",
                    "x-session" to "proxy-session"
                )
            )
        )

    /** Creates a stub configured for non-proxy mode (full processing). */
    protected fun nonProxyStub(
        session: String = "test-session",
        token: String = "test-token",
        eduId: String = "test-edu-id",
        channel: String = "test-channel",
        platform: String = "test-platform"
    ): IvrServiceCoroutineStub = clientStub.withInterceptors(
        MetadataInterceptor(
            mapOf(
                "proxy" to "false",
                "x-session" to session,
                "x-token" to token,
                "x-eduid" to eduId,
                "x-channel" to channel,
                "x-platform" to platform
            )
        )
    )
}
