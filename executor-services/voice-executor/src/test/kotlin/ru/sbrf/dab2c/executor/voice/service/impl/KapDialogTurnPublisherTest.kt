package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.SessionConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.voice.model.ProcessingState

class KapDialogTurnPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var processingState: MutableStateFlow<ProcessingState>
    private lateinit var publisher: KapDialogTurnPublisher

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishDialog(any()) } returns Unit
        processingState = MutableStateFlow(createServingState())
        publisher = KapDialogTurnPublisher(kapProducerClient, processingState)
    }

    @Test
    fun `should publish dialog to KAP`() = runTest {
        publisher.publishDialogTurn("Hello", "Hi there")

        coVerify(exactly = 1) { kapProducerClient.publishDialog(any()) }
    }

    @Test
    fun `should publish dialog with correct text`() = runTest {
        val dialogSlot = slot<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

        publisher.publishDialogTurn("How are you?", "I am fine!")

        val capturedDialog = dialogSlot.captured
        assertEquals("How are you?", capturedDialog.data.userMessage.text)
        assertEquals("I am fine!", capturedDialog.data.assistantMessage?.text)
    }

    @Test
    fun `should set correct chatId from conversationId`() = runTest {
        val dialogSlot = slot<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

        publisher.publishDialogTurn("Hello", "Hi")

        val capturedDialog = dialogSlot.captured
        assertEquals("test-conversation-id", capturedDialog.data.userMessage.chatId)
        assertEquals("test-conversation-id", capturedDialog.data.assistantMessage?.chatId)
    }

    @Test
    fun `should set session info fields from DaSessionInfo`() = runTest {
        val dialogSlot = slot<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

        publisher.publishDialogTurn("Hello", "Hi")

        val userMessage = dialogSlot.captured.data.userMessage
        assertEquals("test-session-id", userMessage.sessionId)
        assertEquals("test-user-id", userMessage.userId)
        assertEquals("test-ucp-id", userMessage.ucpId)
        assertEquals("mobile", userMessage.surface)
        assertEquals("sbol", userMessage.channel)
        assertEquals("ios", userMessage.platform)
        assertEquals("+03:00", userMessage.timeZone)
    }

    @Test
    fun `should not publish when not in Serving state`() = runTest {
        processingState.value = ProcessingState.AwaitingContext

        publisher.publishDialogTurn("Hello", "Hi")

        coVerify(exactly = 0) { kapProducerClient.publishDialog(any()) }
    }

    @Test
    fun `should link messages with previousMessageId`() = runTest {
        val dialogs = mutableListOf<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogs)) } returns Unit

        publisher.publishDialogTurn("First", "Answer 1")
        publisher.publishDialogTurn("Second", "Answer 2")

        val firstDialog = dialogs[0]
        val secondDialog = dialogs[1]

        assertNotNull(firstDialog.data.userMessage.id)
        assertEquals(
            firstDialog.data.userMessage.id,
            firstDialog.data.assistantMessage?.previousMessageId
        )
        assertEquals(
            firstDialog.data.assistantMessage?.id,
            secondDialog.data.userMessage.previousMessageId
        )
    }

    @Test
    fun `should set inputType to voice`() = runTest {
        val dialogSlot = slot<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

        publisher.publishDialogTurn("Hello", "Hi")

        assertEquals("voice", dialogSlot.captured.data.userMessage.inputType)
    }

    @Test
    fun `should set streamStatus to completed`() = runTest {
        val dialogSlot = slot<DialogEnvelope>()
        coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

        publisher.publishDialogTurn("Hello", "Hi")

        assertEquals("completed", dialogSlot.captured.data.assistantMessage?.streamStatus)
    }

    private fun createServingState(): ProcessingState.Serving = ProcessingState.Serving(
        contextData = mockk(),
        agentConfiguration = AgentConfiguration(
            name = "test-agent",
            type = "voice",
            functionalSubsystemCi = "test-ci",
            description = "Test agent",
            entryPoints = emptyList(),
            ufsServiceAvailable = true,
            canAccessUserInfo = true,
            toolsMeta = emptyList(),
            neighboursAgentMeta = emptyList(),
            toggles = emptyMap()
        ),
        sessionConfiguration = SessionConfiguration(
            channel = "sbol",
            platform = "ios"
        ),
        conversationId = "test-conversation-id",
        daSessionInfo = DaSessionInfo(
            meta = DaSessionMeta(
                sessionId = "test-session-id",
                userId = "test-user-id",
                ucpId = "test-ucp-id",
                ufsHost = "test-host"
            ),
            common = DaSessionCommon(
                block = "test-block",
                channel = "sbol",
                surface = "mobile",
                platform = "ios",
                sdkVersion = "1.0",
                entryPoint = "main",
                appVersion = "2.0",
                channelVersion = "3.0",
                appSource = "ucf",
                timeZone = "+03:00"
            ),
            userInfo = DaSessionUserInfo(
                firstName = "Test",
                patrName = "User",
                birthDay = "2000-01-01",
                segmentCodeType = "A",
                ucpId = "test-ucp-id"
            )
        ),
        functionRegistry = FunctionPerformers(emptyMap())
    )
}
