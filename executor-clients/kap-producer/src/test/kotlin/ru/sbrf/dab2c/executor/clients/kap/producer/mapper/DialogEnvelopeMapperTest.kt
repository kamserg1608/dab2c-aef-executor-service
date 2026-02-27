package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

class DialogEnvelopeMapperTest {

    @Test
    fun `should map dialog turn to DialogEnvelope`() {
        val data = createTestDialogTurnData()

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        assertEquals("envelope-123", envelope.id)
        assertEquals("1.2.0", envelope.version)
        assertEquals(1705849200L, envelope.date)
    }

    @Test
    fun `should map user message with correct fields`() {
        val data = createTestDialogTurnData(previousMessageId = "prev-msg-000")

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        val userMessage = envelope.data.userMessage
        assertEquals("chat-abc", userMessage.chatId)
        assertEquals("user-msg-456", userMessage.id)
        assertEquals(1705849200L, userMessage.dateCreated)
        assertEquals("Hello, how are you?", userMessage.text)
        assertEquals("voice", userMessage.inputType)
        assertEquals("prev-msg-000", userMessage.previousMessageId)
    }

    @Test
    fun `should map user message session info from DaSessionInfo`() {
        val data = createTestDialogTurnData()

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        val userMessage = envelope.data.userMessage
        assertEquals("test-session-id", userMessage.sessionId)
        assertEquals("test-user-id", userMessage.userId)
        assertEquals("test-ucp-id", userMessage.ucpId)
        assertEquals("mobile", userMessage.surface)
        assertEquals("sbol", userMessage.channel)
        assertEquals("ios", userMessage.platform)
        assertEquals("+03:00", userMessage.timeZone)
        assertEquals("ucf", userMessage.appSource)
        assertEquals("main", userMessage.entryPoint)
        assertEquals("2.0", userMessage.appVersion)
        assertEquals("3.0", userMessage.channelVersion)
    }

    @Test
    fun `should map assistant message with correct fields`() {
        val data = createTestDialogTurnData()

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        val assistantMessage = envelope.data.assistantMessage
        assertEquals("chat-abc", assistantMessage?.chatId)
        assertEquals("assistant-msg-789", assistantMessage?.id)
        assertEquals(1705849200L, assistantMessage?.dateCreated)
        assertEquals("I am fine, thank you!", assistantMessage?.text)
        assertEquals("test-session-id", assistantMessage?.sessionId)
        assertEquals("user-msg-456", assistantMessage?.previousMessageId)
        assertEquals("completed", assistantMessage?.streamStatus)
        assertEquals(false, assistantMessage?.isIdp)
    }

    @Test
    fun `should handle blank session fields as null`() {
        val sessionInfo = DaSessionInfo(
            meta = DaSessionMeta(
                sessionId = "",
                userId = "  ",
                ucpId = "test-ucp",
                ufsHost = "test-host"
            ),
            common = DaSessionCommon(
                block = "",
                channel = "sbol",
                surface = "",
                platform = "ios",
                sdkVersion = "",
                entryPoint = "",
                appVersion = "",
                channelVersion = "",
                appSource = "",
                timeZone = ""
            ),
            userInfo = DaSessionUserInfo(
                firstName = "Test",
                patrName = "User",
                birthDay = "2000-01-01",
                segmentCodeType = "A",
                ucpId = "test-ucp"
            )
        )

        val data = createTestDialogTurnData(daSessionInfo = sessionInfo)

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        val userMessage = envelope.data.userMessage
        assertNull(userMessage.sessionId)
        assertNull(userMessage.userId)
        assertEquals("test-ucp", userMessage.ucpId)
        assertNull(userMessage.surface)
        assertEquals("sbol", userMessage.channel)
        assertEquals("ios", userMessage.platform)
        assertNull(userMessage.timeZone)
    }

    @Test
    fun `should handle null previousMessageId for first turn`() {
        val data = createTestDialogTurnData(previousMessageId = null)

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        assertNull(envelope.data.userMessage.previousMessageId)
    }

    @Test
    fun `should map agentId as array with ci object`() {
        val data = createTestDialogTurnData(agentCi = "agent-ci-12345")

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        val assistantMessage = envelope.data.assistantMessage
        assertEquals(1, assistantMessage?.agentId?.size)
        assertEquals("agent-ci-12345", assistantMessage?.agentId?.first()?.ci)
    }

    @Test
    fun `should map assistantResponseTime in milliseconds`() {
        val data = createTestDialogTurnData(assistantResponseTime = 1500L)

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        assertEquals(1500L, envelope.data.assistantMessage?.assistantResponseTime)
    }

    @Test
    fun `should map requestId to assistant message`() {
        val data = createTestDialogTurnData(requestId = "test-request-id-123")

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        assertEquals("test-request-id-123", envelope.data.assistantMessage?.requestId)
    }

    @Test
    fun `should handle null requestId`() {
        val data = createTestDialogTurnData(requestId = null)

        val envelope = DialogEnvelopeMapper.toDialogEnvelope(data)

        assertNull(envelope.data.assistantMessage?.requestId)
    }

    private fun createTestDialogTurnData(
        previousMessageId: String? = null,
        daSessionInfo: DaSessionInfo = createTestSessionInfo(),
        agentCi: String = "test-agent-ci",
        assistantResponseTime: Long = 1000L,
        requestId: String? = null
    ): DialogTurnData = DialogTurnData(
        envelopeId = "envelope-123",
        userMessageId = "user-msg-456",
        assistantMessageId = "assistant-msg-789",
        inputText = "Hello, how are you?",
        outputText = "I am fine, thank you!",
        chatId = "chat-abc",
        timestamp = 1705849200L,
        previousMessageId = previousMessageId,
        daSessionInfo = daSessionInfo,
        agentCi = agentCi,
        assistantResponseTime = assistantResponseTime,
        requestId = requestId
    )

    private fun createTestSessionInfo(): DaSessionInfo = DaSessionInfo(
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
