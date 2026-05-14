package ru.sbrf.dab2c.executor.it.tests.grpc

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnEvent
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogTurnExtra
import ru.sbrf.dab2c.executor.clients.kap.producer.model.ErrorPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionCallPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.FunctionResultPayload
import ru.sbrf.dab2c.executor.clients.kap.producer.model.WarningPayload
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.audioRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.audioResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.errorResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.finalAudioResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.functionCallingResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.warningResponse
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.withConsumer
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubsWithKapExtra
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubGigaAgentFunctions
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

private const val DIALOGS_TOPIC = "dab2c-core-dialogs"

/**
 * Integration tests for the `extra` field in dialog messages published to KAP.
 * Verifies events (function_call, function_result, warning, error) and
 * message boundary timestamps (userMessageStartTS, userMessageEndTS,
 * assistantMessageStartTS, assistantMessageEndTS).
 */
class DialogExtraPublishingIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should not include extra when toggle is disabled`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "extra-disabled-${System.currentTimeMillis()}"

        val records = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))
                    mock.awaitRequest { it.hasSettings() }

                    session.sendRequest(audioRequest())

                    mock.sendResponse(inputTranscriptionResponse("Hello"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(outputTranscriptionResponse("Hi"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(audioResponse(1))
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(finalAudioResponse())
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(inputTranscriptionResponse("Next"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(records).isNotEmpty()
        assertThat(records.first().data.extra).isNull()
    }

    @Test
    fun `should include timestamps in extra when toggle is enabled`() = runItTest {
        setupStubsWithKapExtra(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "extra-timestamps-${System.currentTimeMillis()}"

        val records = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))
                    mock.awaitRequest { it.hasSettings() }

                    session.sendRequest(audioRequest())

                    mock.sendResponse(inputTranscriptionResponse("Hello"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(audioResponse(1))
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(outputTranscriptionResponse("Hi"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(finalAudioResponse())
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(inputTranscriptionResponse("Next"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(records).isNotEmpty()

        val extra = parseExtra(records.first())
        assertThat(extra.userMessageStartTS).isNotNull()
        assertThat(extra.userMessageEndTS).isNotNull()
        assertThat(extra.assistantMessageStartTS).isNotNull()
        assertThat(extra.assistantMessageEndTS).isNotNull()
        assertThat(extra.userMessageStartTS!!).isLessThanOrEqualTo(extra.userMessageEndTS!!)
        assertThat(extra.assistantMessageStartTS!!).isLessThanOrEqualTo(extra.assistantMessageEndTS!!)
    }

    @Test
    fun `should include function_call and function_result events in extra`() = runItTest {
        setupStubsWithKapExtra(efsAdapterMock, gigaVoiceAgentMock, withFunctions = true)
        gigaVoiceAgentMock.stubGigaAgentFunctions("get_account_balance", """{"balance": 1000}""")

        val testChatId = "extra-functions-${System.currentTimeMillis()}"

        val records = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))
                    mock.awaitRequest { it.hasSettings() }

                    session.sendRequest(audioRequest())

                    mock.sendResponse(inputTranscriptionResponse("Check balance"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(
                        functionCallingResponse("get_account_balance", """{"account_id": "123"}""")
                    )
                    wireMock.awaitPostCall("/functions")

                    mock.awaitRequest { it.hasFunctionResult() }

                    mock.sendResponse(outputTranscriptionResponse("Your balance is 1000"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(finalAudioResponse())
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(inputTranscriptionResponse("Thanks"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(records).isNotEmpty()

        val extraMap: Map<String, Any?> =
            ObjectMappers.MAPPER.readValue(records.first().data.extra ?: error("extra missing"))
        assertMatchesGolden(
            maskNonDeterministic(extraMap, EXTRA_NON_DETERMINISTIC_FIELDS),
            "golden/dialog-extra/function-call-events.json"
        )
    }

    @Test
    fun `should include warning and error events in extra`() = runItTest {
        setupStubsWithKapExtra(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "extra-warn-error-${System.currentTimeMillis()}"

        val records = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))
                    mock.awaitRequest { it.hasSettings() }

                    session.sendRequest(audioRequest())

                    mock.sendResponse(inputTranscriptionResponse("Hello"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(warningResponse("low confidence"))
                    session.awaitResponse { it.hasWarning() }

                    mock.sendResponse(errorResponse(400, "Bad Request"))
                    session.awaitResponse { it.hasError() }

                    mock.sendResponse(outputTranscriptionResponse("Sorry"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(finalAudioResponse())
                    session.awaitResponse { it.hasOutput() }

                    mock.sendResponse(inputTranscriptionResponse("Bye"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(records).isNotEmpty()

        val extraMap: Map<String, Any?> =
            ObjectMappers.MAPPER.readValue(records.first().data.extra ?: error("extra missing"))
        assertMatchesGolden(
            maskNonDeterministic(extraMap, EXTRA_NON_DETERMINISTIC_FIELDS),
            "golden/dialog-extra/warning-error-events.json"
        )
    }

    private fun parseExtra(envelope: DialogEnvelope): DialogTurnExtra {
        val extraJson = envelope.data.extra
        assertThat(extraJson).isNotNull()
        val tree = ObjectMappers.MAPPER.readTree(extraJson)
        val events = tree["events"]?.map { parseEvent(it) } ?: emptyList()
        return DialogTurnExtra(
            events = events,
            userMessageStartTS = tree["userMessageStartTS"]?.asLong(),
            userMessageEndTS = tree["userMessageEndTS"]?.asLong(),
            assistantMessageStartTS = tree["assistantMessageStartTS"]?.asLong(),
            assistantMessageEndTS = tree["assistantMessageEndTS"]?.asLong()
        )
    }

    private fun parseEvent(node: JsonNode): DialogTurnEvent {
        val eventName = node["eventName"].asText()
        val timestamp = node["timestamp"].asLong()
        val payloadNode = node["payload"]
        val payload = when (eventName) {
            "function_call" -> ObjectMappers.MAPPER.treeToValue(payloadNode, FunctionCallPayload::class.java)
            "function_result" -> ObjectMappers.MAPPER.treeToValue(payloadNode, FunctionResultPayload::class.java)
            "warning" -> ObjectMappers.MAPPER.treeToValue(payloadNode, WarningPayload::class.java)
            "error" -> ObjectMappers.MAPPER.treeToValue(payloadNode, ErrorPayload::class.java)
            else -> error("Unknown event: $eventName")
        }
        return DialogTurnEvent(eventName = eventName, timestamp = timestamp, payload = payload)
    }

    private companion object {
        /** Non-deterministic timestamp fields in DialogTurnExtra (per-event + per-turn boundary). */
        private val EXTRA_NON_DETERMINISTIC_FIELDS = setOf(
            "timestamp",
            "userMessageStartTS",
            "userMessageEndTS",
            "assistantMessageStartTS",
            "assistantMessageEndTS"
        )
    }
}
