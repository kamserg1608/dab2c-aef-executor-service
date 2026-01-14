package ru.sbrf.dab2c.executor.it

import com.fasterxml.jackson.databind.DeserializationFeature
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.MethodDescriptor
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
import jakarta.servlet.ServletContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import ru.sbrf.dab2c.executor.application.ApplicationEntryPoint
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrServiceGrpcKt.IvrServiceCoroutineStub

@SpringBootTest(
    classes = [ApplicationEntryPoint::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["STUB", "stubMode", "test"])
@EnableWireMock(
    ConfigureWireMock(name = "gigaVoiceAgent", baseUrlProperties = ["giga.voice.agent.client.baseUrl"]),
    ConfigureWireMock(name = "efsAdapter", baseUrlProperties = ["efs.adapter.baseUrl"])
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseGigaVoiceIntegrationTest {

    @Value("\${grpc.server.port}")
    private var grpcServerPort: Int = 0

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var servletContext: ServletContext

    @Autowired
    protected lateinit var mockGigaVoiceService: MockGigaVoiceService

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
        clientStub.withInterceptors(MetadataInterceptor(mapOf("proxy" to "true")))

    /** Creates a stub configured for non-proxy mode (full processing). */
    protected fun nonProxyStub(
        session: String = "test-session",
        token: String = "test-token"
    ): IvrServiceCoroutineStub = clientStub.withInterceptors(
        MetadataInterceptor(
            mapOf(
                "proxy" to "false",
                "session" to session,
                "token" to token
            )
        )
    )
}

/** gRPC interceptor that adds metadata headers to outgoing calls. */
private class MetadataInterceptor(
    private val headers: Map<String, String>
) : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, metadata: Metadata) {
                headers.forEach { (key, value) ->
                    metadata.put(Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER), value)
                }
                super.start(responseListener, metadata)
            }
        }
    }
}
