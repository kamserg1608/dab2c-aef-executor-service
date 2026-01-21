package ru.sbrf.dab2c.executor.it.tests.grpc.kap

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AssistantMessage
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.UserMessage
import ru.sbrf.dab2c.executor.it.support.kafka.KafkaTestSupport.awaitRecords
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import java.util.UUID

private const val DIALOGS_TOPIC = "dab2c-core-dialogs"
private const val AGENTS_TOPIC = "dab2c-agents"

/**
 * Integration tests for KapProducerClient using embedded Kafka.
 */
class KapProducerClientIntegrationTest : BaseGigaVoiceIntegrationTest() {

    @Autowired
    private lateinit var kapProducerClient: KapProducerClient

    @Test
    fun `should publish dialog to dialogs topic`() = runTest {
        val dialog = createTestDialogEnvelope()

        kapProducerClient.publishDialog(dialog)

        val matchingRecords = embeddedKafkaBroker.awaitRecords<DialogEnvelope>(
            topic = DIALOGS_TOPIC,
            filter = { it.id == dialog.id }
        )

        assertEquals(1, matchingRecords.size)

        val receivedDialog = matchingRecords.first()
        assertEquals(dialog.id, receivedDialog.id)
        assertEquals(dialog.version, receivedDialog.version)
        assertEquals(dialog.data.userMessage.text, receivedDialog.data.userMessage.text)
        assertEquals(dialog.data.assistantMessage?.text, receivedDialog.data.assistantMessage?.text)
    }

    @Test
    fun `should publish agent analytics to agents topic`() = runTest {
        val analytics = createTestAgentAnalyticsEnvelope()

        kapProducerClient.publishAgentAnalytics(analytics)

        val matchingRecords = embeddedKafkaBroker.awaitRecords<AgentAnalyticsEnvelope>(
            topic = AGENTS_TOPIC,
            filter = { it.id == analytics.id }
        )

        assertEquals(1, matchingRecords.size)

        val receivedAnalytics = matchingRecords.first()
        assertEquals(analytics.id, receivedAnalytics.id)
        assertEquals(analytics.version, receivedAnalytics.version)
        assertEquals(analytics.agentName, receivedAnalytics.agentName)
        assertEquals(analytics.sessionId, receivedAnalytics.sessionId)
    }

    private fun createTestDialogEnvelope(): DialogEnvelope {
        val userMessage = UserMessage(
            chatId = UUID.randomUUID().toString(),
            id = UUID.randomUUID().toString(),
            dateCreated = System.currentTimeMillis() / 1000,
            text = "Test user message",
            sessionId = UUID.randomUUID().toString()
        )
        val assistantMessage = AssistantMessage(
            chatId = userMessage.chatId,
            id = UUID.randomUUID().toString(),
            dateCreated = System.currentTimeMillis() / 1000,
            text = "Test assistant response",
            sessionId = userMessage.sessionId,
            previousMessageId = userMessage.id,
            streamStatus = "completed"
        )
        return DialogEnvelope(
            id = UUID.randomUUID().toString(),
            version = "0.0.1",
            date = System.currentTimeMillis() / 1000,
            data = DialogData(
                userMessage = userMessage,
                assistantMessage = assistantMessage
            )
        )
    }

    private fun createTestAgentAnalyticsEnvelope(): AgentAnalyticsEnvelope {
        return AgentAnalyticsEnvelope(
            version = "1.1.0",
            id = UUID.randomUUID().toString(),
            date = System.currentTimeMillis() / 1000,
            sessionId = UUID.randomUUID().toString(),
            conversationId = UUID.randomUUID().toString(),
            ucpId = UUID.randomUUID().toString(),
            block = "test-block",
            channel = "test-channel",
            agentName = "test-agent",
            dataVersion = "1.0.0",
            agentCi = "test-ci",
            size = "0"
        )
    }
}
