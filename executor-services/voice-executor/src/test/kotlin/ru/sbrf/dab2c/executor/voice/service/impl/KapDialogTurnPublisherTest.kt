package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.voice.grpc.context.MetadataElement
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

class KapDialogTurnPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var processingState: MutableStateFlow<ProcessingState>
    private lateinit var publisher: KapDialogTurnPublisher
    private lateinit var metadataContext: MetadataElement

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishDialog(any()) } returns Unit
        processingState = MutableStateFlow(createServingState())
        publisher = KapDialogTurnPublisher(kapProducerClient, processingState)

        val metadata = RequestMetadata(emptyMap())
        metadata._daSessionInfo = createDaSessionInfo()
        metadataContext = MetadataElement(metadata)
    }

    @Test
    fun `should publish dialog to KAP`() = runTest {
        withContext(metadataContext) {
            publisher.publishDialogTurn("Hello", "Hi there", 1000L)

            coVerify(exactly = 1) { kapProducerClient.publishDialog(any()) }
        }
    }

    @Test
    fun `should publish dialog with correct text`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("How are you?", "I am fine!", 1000L)

            val capturedDialog = dialogSlot.captured
            assertEquals("How are you?", capturedDialog.data.userMessage.text)
            assertEquals("I am fine!", capturedDialog.data.assistantMessage?.text)
        }
    }

    @Test
    fun `should set correct chatId from conversationId`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            val capturedDialog = dialogSlot.captured
            assertEquals("test-conversation-id", capturedDialog.data.userMessage.chatId)
            assertEquals("test-conversation-id", capturedDialog.data.assistantMessage?.chatId)
        }
    }

    @Test
    fun `should set session info fields from context`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            val userMessage = dialogSlot.captured.data.userMessage
            assertEquals("test-session-id", userMessage.sessionId)
            assertEquals("test-user-id", userMessage.userId)
            assertEquals("test-ucp-id", userMessage.ucpId)
            assertEquals("mobile", userMessage.surface)
            assertEquals("sbol", userMessage.channel)
            assertEquals("ios", userMessage.platform)
            assertEquals("+03:00", userMessage.timeZone)
        }
    }

    @Test
    fun `should not publish when not in Serving state`() = runTest {
        withContext(metadataContext) {
            processingState.value = ProcessingState.AwaitingContext

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            coVerify(exactly = 0) { kapProducerClient.publishDialog(any()) }
        }
    }

    @Test
    fun `should link messages with previousMessageId`() = runTest {
        withContext(metadataContext) {
            val dialogs = mutableListOf<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogs)) } returns Unit

            publisher.publishDialogTurn("First", "Answer 1", 1000L)
            publisher.publishDialogTurn("Second", "Answer 2", 1500L)

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
    }

    @Test
    fun `should set inputType to voice`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            assertEquals("voice", dialogSlot.captured.data.userMessage.inputType)
        }
    }

    @Test
    fun `should set streamStatus to completed`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            assertEquals("completed", dialogSlot.captured.data.assistantMessage?.streamStatus)
        }
    }

    @Test
    fun `should set agentId with CI from agent configuration`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 1000L)

            val agentId = dialogSlot.captured.data.assistantMessage?.agentId
            assertEquals(1, agentId?.size)
            assertEquals("test-ci", agentId?.first()?.ci)
        }
    }

    @Test
    fun `should set assistantResponseTime from parameter`() = runTest {
        withContext(metadataContext) {
            val dialogSlot = slot<DialogEnvelope>()
            coEvery { kapProducerClient.publishDialog(capture(dialogSlot)) } returns Unit

            publisher.publishDialogTurn("Hello", "Hi", 2500L)

            assertEquals(2500L, dialogSlot.captured.data.assistantMessage?.assistantResponseTime)
        }
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
        conversationId = "test-conversation-id",
        functionRegistry = FunctionPerformers(emptyMap())
    )

    private fun createDaSessionInfo(): DaSessionInfo = DaSessionInfo(
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
    )
}
