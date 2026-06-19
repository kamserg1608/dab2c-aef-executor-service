package ru.sbrf.dab2c.executor.it.tests.grpc.tracing

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.TracingKafkaConsumer.collectTraceSpans
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.assertSpans

/** Downstream EOS-style abnormal close is a tolerant termination: the SDK spans stay non-error. */
class TracingTolerantCloseTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `downstream unexpected EOS close keeps session spans non-error`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = "tracing-eos") {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest("tracing-eos"))
                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("hello"))
                    session.awaitResponse { it.hasInputTranscription() }
                    mock.sendResponse(outputTranscriptionResponse("hi"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.completeResponsesWithError(
                        StatusRuntimeException(
                            Status.INTERNAL.withDescription(
                                "Received unexpected EOS on empty DATA frame from server"
                            )
                        )
                    )
                }
            }
        }

        assertThat(spans).isNotEmpty
        assertSpans(spans) {
            val downstream = allOfKind("output_request").first { it.name == "downstream gigavoice stream" }
            assertAttributeEquals(downstream, "aef.response.status_code", "OK")

            assertThat(ofKind("start_agent").statusCode)
                .withFailMessage("start_agent must not be ERROR for tolerant EOS close")
                .isNotEqualTo("STATUS_CODE_ERROR")
        }
    }
}
