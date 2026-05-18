package ru.sbrf.dab2c.executor.it.tests.grpc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.withConsumer
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.testing.golden.assertMatchesGolden
import ru.sbrf.dab2c.executor.library.testing.golden.maskNonDeterministic

private const val DIALOGS_TOPIC = "dab2c-core-dialogs"

/**
 * Integration tests for dialog publishing to KAP through the full transcription flow.
 */
class DialogPublishingIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should publish dialog to KAP when dialog turn completes`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "single-turn-test-${System.currentTimeMillis()}"

        val dialogRecords = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))

                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("Hello"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(outputTranscriptionResponse("Hi there"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(inputTranscriptionResponse("How are you?"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(dialogRecords).isNotEmpty()

        val receivedDialog = dialogRecords.first()
        assertThat(receivedDialog.version).isEqualTo("1.2.0")
        assertThat(receivedDialog.data.userMessage.text).isEqualTo("Hello")
        assertThat(receivedDialog.data.assistantMessage?.text).isEqualTo("Hi there")
        assertThat(receivedDialog.data.userMessage.chatId).isEqualTo(testChatId)

        val assistantMessage = receivedDialog.data.assistantMessage
        assertThat(assistantMessage?.agentIds).isNotNull
        assertThat(assistantMessage?.agentIds).hasSize(1)
        assertThat(assistantMessage?.agentIds?.first()?.ci).isNotBlank()
        assertThat(assistantMessage?.assistantResponseTime).isNotNull
        assertThat(assistantMessage?.assistantResponseTime).isGreaterThanOrEqualTo(0L)
    }

    @Test
    fun `should publish multiple dialogs for multiple turns`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "multi-turn-test-${System.currentTimeMillis()}"

        val dialogRecords = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
                    session.sendRequest(contextRequest())
                    session.sendRequest(settingsRequest(testChatId))

                    mock.awaitRequest { it.hasSettings() }

                    mock.sendResponse(inputTranscriptionResponse("First question"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(outputTranscriptionResponse("First answer"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(inputTranscriptionResponse("Second question"))
                    session.awaitResponse { it.hasInputTranscription() }

                    mock.sendResponse(outputTranscriptionResponse("Second answer"))
                    session.awaitResponse { it.hasOutputTranscription() }

                    mock.sendResponse(inputTranscriptionResponse("Third question"))
                    session.awaitResponse { it.hasInputTranscription() }
                }
            }
        }

        assertThat(dialogRecords.size).isGreaterThanOrEqualTo(2)

        val firstDialog = dialogRecords.find { it.data.userMessage.previousMessageId == null }
            ?: error("First dialog (with previousMessageId=null) not found in $dialogRecords")
        val secondDialog = dialogRecords.find {
            it.data.userMessage.previousMessageId == firstDialog.data.assistantMessage?.id
        } ?: error("Second dialog (linked to first by previousMessageId) not found in $dialogRecords")

        assertMatchesGolden(
            maskNonDeterministic(firstDialog.asMap(), DIALOG_NON_DETERMINISTIC_FIELDS),
            "golden/dialog/first-turn.json"
        )
        assertMatchesGolden(
            maskNonDeterministic(secondDialog.asMap(), DIALOG_NON_DETERMINISTIC_FIELDS),
            "golden/dialog/second-turn.json"
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun DialogEnvelope.asMap(): Map<String, Any?> =
        ObjectMappers.MAPPER.convertValue(this, Map::class.java) as Map<String, Any?>

    private companion object {
        /** Non-deterministic fields in a DialogEnvelope: UUIDs, timestamps, generated chat ids. */
        private val DIALOG_NON_DETERMINISTIC_FIELDS = setOf(
            "id",
            "userMessageId",
            "assistantMessageId",
            "previousMessageId",
            "requestId",
            "chatId",
            "date",
            "dateCreated",
            "assistantResponseTime",
            "sessionId",
            "userId",
            "ucpId"
        )
    }
}
