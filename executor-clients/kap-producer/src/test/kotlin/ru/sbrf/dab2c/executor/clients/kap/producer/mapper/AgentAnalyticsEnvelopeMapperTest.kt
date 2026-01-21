package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

class AgentAnalyticsEnvelopeMapperTest {

    @Test
    fun `should map analytics data to AgentAnalyticsEnvelope with required fields`() {
        val data = createTestAnalyticsData()

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertEquals("1.1.0", envelope.version)
        assertEquals("envelope-123", envelope.id)
        assertEquals(1705849200L, envelope.date)
        assertEquals("test-session-id", envelope.sessionId)
        assertEquals("conversation-456", envelope.conversationId)
        assertEquals("test-ucp-id", envelope.ucpId)
        assertEquals("test-block", envelope.block)
        assertEquals("sbol", envelope.channel)
        assertEquals("test-agent", envelope.agentName)
        assertEquals("1.0.0", envelope.dataVersion)
        assertEquals("test-ci-123", envelope.agentCi)
    }

    @Test
    fun `should include optional fields when present`() {
        val data = createTestAnalyticsData(requestId = "request-789")

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertEquals("request-789", envelope.requestId)
        assertEquals("ucf", envelope.appSource)
        assertEquals("ios", envelope.platform)
    }

    @Test
    fun `should handle blank optional fields as null`() {
        val sessionInfo = DaSessionInfo(
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
                platform = "",
                sdkVersion = "1.0",
                entryPoint = "main",
                appVersion = "2.0",
                channelVersion = "3.0",
                appSource = "  ",
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

        val data = createTestAnalyticsData(daSessionInfo = sessionInfo)

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertNull(envelope.appSource)
        assertNull(envelope.platform)
    }

    @Test
    fun `should pass data string directly to envelope`() {
        val analyticsData = """{"metric1":"value1","count":42}"""
        val data = createTestAnalyticsData(data = analyticsData)

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertEquals("""{"metric1":"value1","count":42}""", envelope.data)
    }

    @Test
    fun `should calculate size in megabytes`() {
        val analyticsData = """{"key":"value"}"""
        val data = createTestAnalyticsData(data = analyticsData)

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertEquals("0.000015", envelope.size)
    }

    @Test
    fun `should handle null requestId`() {
        val data = createTestAnalyticsData(requestId = null)

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(data)

        assertNull(envelope.requestId)
    }

    private fun createTestAnalyticsData(
        requestId: String? = null,
        data: String = """{"test":"data"}""",
        daSessionInfo: DaSessionInfo = createTestSessionInfo()
    ): AgentAnalyticsData = AgentAnalyticsData(
        envelopeId = "envelope-123",
        timestamp = 1705849200L,
        conversationId = "conversation-456",
        requestId = requestId,
        dataVersion = "1.0.0",
        data = data,
        daSessionInfo = daSessionInfo,
        agentConfiguration = createTestAgentConfiguration()
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

    private fun createTestAgentConfiguration(): AgentConfiguration = AgentConfiguration(
        name = "test-agent",
        type = "voice",
        functionalSubsystemCi = "test-ci-123",
        description = "Test agent description",
        entryPoints = listOf("main"),
        ufsServiceAvailable = true,
        canAccessUserInfo = true,
        toolsMeta = emptyList(),
        neighboursAgentMeta = emptyList(),
        toggles = emptyMap()
    )
}
