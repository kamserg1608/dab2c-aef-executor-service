package ru.sbrf.dab2c.executor.it

import GigaVoiceProtocol.GigaVoiceServiceGrpcKt.GigaVoiceServiceCoroutineStub
import com.fasterxml.jackson.databind.DeserializationFeature
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
import org.springframework.test.context.ActiveProfiles
import ru.sbrf.dab2c.executor.application.ApplicationEntryPoint
import javax.servlet.ServletContext

@SpringBootTest(
    classes = [ApplicationEntryPoint::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles(profiles = ["STUB", "stubMode", "test"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseGigaVoiceIntegrationTest {

    @Value("\${grpc.server.port}")
    private var grpcServerPort: Int = 0

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var servletContext: ServletContext

    @Autowired
    protected lateinit var mockDownstreamService: MockGigaVoiceDownstreamService

    private lateinit var clientChannel: ManagedChannel
    protected lateinit var clientStub: GigaVoiceServiceCoroutineStub

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
