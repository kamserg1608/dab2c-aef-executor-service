package ru.sbrf.dab2c.executor.it.tests.grpc.kap

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.inputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.fixtures.IvrRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.IvrRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.withConsumer
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupFullModeStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

private const val DIALOGS_TOPIC = "dab2c-core-dialogs"

/**
 * Integration tests for dialog publishing to KAP through the full transcription flow.
 */
class DialogPublishingIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `should publish dialog to KAP when dialog turn completes`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "single-turn-test-${System.currentTimeMillis()}"

        val dialogRecords = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
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
        assertThat(receivedDialog.data.userMessage.text).isEqualTo("Hello")
        assertThat(receivedDialog.data.assistantMessage?.text).isEqualTo("Hi there")
        assertThat(receivedDialog.data.userMessage.chatId).isEqualTo(testChatId)
    }

    @Test
    fun `should publish multiple dialogs for multiple turns`() = runItTest {
        setupFullModeStubs(efsAdapterMock, gigaVoiceAgentMock)

        val testChatId = "multi-turn-test-${System.currentTimeMillis()}"

        val dialogRecords = embeddedKafkaBroker.withConsumer<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.data.userMessage.chatId == testChatId }
        ) {
            runItTest {
                withSession(nonProxyStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
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
        assertThat(firstDialog).isNotNull
        assertThat(firstDialog!!.data.userMessage.text).isEqualTo("First question")
        assertThat(firstDialog.data.assistantMessage?.text).isEqualTo("First answer")

        val secondDialog = dialogRecords.find {
            it.data.userMessage.previousMessageId == firstDialog.data.assistantMessage?.id
        }
        assertThat(secondDialog).isNotNull
        assertThat(secondDialog!!.data.userMessage.text).isEqualTo("Second question")
        assertThat(secondDialog.data.assistantMessage?.text).isEqualTo("Second answer")
    }
}
