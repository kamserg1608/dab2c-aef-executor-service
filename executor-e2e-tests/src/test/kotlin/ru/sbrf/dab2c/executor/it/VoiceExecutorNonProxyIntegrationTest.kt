package ru.sbrf.dab2c.executor.it

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.grpc.Metadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.wiremock.spring.ConfigureWireMock
import org.wiremock.spring.EnableWireMock
import org.wiremock.spring.InjectWireMock
import ru.sbrf.dab2c.executor.clients.ivr.proto.AudioSettings
import ru.sbrf.dab2c.executor.clients.ivr.proto.IvrRequest
import ru.sbrf.dab2c.executor.clients.ivr.proto.Settings
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test for voice executor with proxyMode=false.
 * Uses WireMock to stub HTTP clients (ConfiguratorClient, GigaVoiceAgentClient).
 */
@ActiveProfiles("non-proxy-test", "STUB")
@DirtiesContext
@EnableWireMock(
    ConfigureWireMock(name = "gigaVoiceAgent", baseUrlProperties = ["giga.voice.agent.client.baseUrl"]),
    ConfigureWireMock(name = "efsAdapter", baseUrlProperties = ["efs.adapter.baseUrl"])
)
class VoiceExecutorNonProxyIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @InjectWireMock("gigaVoiceAgent")
    lateinit var gigaVoiceAgentMock: WireMockServer

    @InjectWireMock("efsAdapter")
    lateinit var efsAdapterMock: WireMockServer

    @BeforeEach
    fun resetWireMocks() {
        gigaVoiceAgentMock.resetAll()
        efsAdapterMock.resetAll()
    }

    @Test
    fun `should process settings through non-proxy flow with HTTP clients`() = runTest(timeout = 30.seconds) {
        // Setup EFS adapter stub
        efsAdapterMock.stubFor(
            post(urlEqualTo("/configurator/rest-agent"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(EFS_ADAPTER_RESPONSE)
                )
        )

        // Setup GigaVoice agent stub
        gigaVoiceAgentMock.stubFor(
            post(urlEqualTo("/settings"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(GIGA_VOICE_SETTINGS_RESPONSE)
                )
        )

        // Create request with metadata
        val requests = flow {
            emit(
                IvrRequest.newBuilder()
                    .setSettings(
                        Settings.newBuilder()
                            .setVoiceCallId("test-call-123")
                            .setAudio(AudioSettings.getDefaultInstance())
                            .build()
                    )
                    .build()
            )
        }

        // Add required headers
        val metadata = Metadata()
        metadata.put(
            Metadata.Key.of("session", Metadata.ASCII_STRING_MARSHALLER),
            "test-session"
        )
        metadata.put(
            Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER),
            "test-token"
        )

        // Execute request with metadata - take first response and don't wait for stream completion
        val stubWithHeaders = clientStub.withInterceptors(MetadataInterceptor(metadata))
        val firstResponse = stubWithHeaders.session(requests).first()

        // Give time for async HTTP calls to complete
        delay(500)

        // Verify HTTP calls were made to external services
        efsAdapterMock.verify(1, postRequestedFor(urlEqualTo("/configurator/rest-agent")))
        gigaVoiceAgentMock.verify(1, postRequestedFor(urlEqualTo("/settings")))

        // Verify we got a response
        assertThat(firstResponse).isNotNull()
    }

    companion object {
        private val EFS_ADAPTER_RESPONSE = """
            {
                "success": true,
                "body": {
                    "test-agent": {
                        "name": "test-agent",
                        "type": "voice",
                        "functional_subsystem_ci": "test-ci",
                        "description": "Test agent",
                        "entry_points": [],
                        "ufs_service_available": true,
                        "can_access_user_info": true,
                        "tools_meta": [],
                        "neighbours_agent_meta": [],
                        "toggles": {}
                    }
                }
            }
        """.trimIndent()

        private val GIGA_VOICE_SETTINGS_RESPONSE = """
            {
                "settings": {
                    "voice_call_id": "test-call-123",
                    "audio": {}
                },
                "performers": {
                    "functions": {}
                }
            }
        """.trimIndent()
    }
}

/**
 * gRPC interceptor that adds metadata to outgoing calls.
 */
private class MetadataInterceptor(
    private val extraMetadata: Metadata
) : io.grpc.ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: io.grpc.MethodDescriptor<ReqT, RespT>,
        callOptions: io.grpc.CallOptions,
        next: io.grpc.Channel
    ): io.grpc.ClientCall<ReqT, RespT> {
        return object : io.grpc.ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            next.newCall(method, callOptions)
        ) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                headers.merge(extraMetadata)
                super.start(responseListener, headers)
            }
        }
    }
}
