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
import ru.sbrf.dab2c.executor.it.support.tracing.callTreeOf
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentSettingsWithCallId
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.testing.tracing.assertSpans

/** Downstream EOS-style abnormal close is a tolerant termination: the SDK spans stay non-error. */
class TracingTolerantCloseTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `downstream unexpected EOS close keeps session spans non-error`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        gigaVoiceAgentMock.stubGigaAgentSettingsWithCallId(EOS_CALL_ID)

        val spans = embeddedKafkaBroker.collectTraceSpans(voiceCallId = EOS_CALL_ID) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(EOS_CALL_ID))
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
        assertSpans(spans.callTreeOf(EOS_CALL_ID)) {
            val downstream = allOfKind("output_request").first { it.name == DOWNSTREAM_SPAN }
            assertAttributeEquals(downstream, "aef.response.status_code", "OK")

            assertThat(ofKind("input_request").statusCode)
                .withFailMessage("input_request must not be ERROR for tolerant EOS close")
                .isNotEqualTo("STATUS_CODE_ERROR")
            assertThat(ofKind("start_agent").statusCode)
                .withFailMessage("start_agent must not be ERROR for tolerant EOS close")
                .isNotEqualTo("STATUS_CODE_ERROR")
        }
    }

    private companion object {
        const val EOS_CALL_ID = "tracing-eos"
        const val DOWNSTREAM_SPAN = "downstream gigavoice stream"
    }
}
