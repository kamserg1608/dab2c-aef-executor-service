package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsData
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

class AgentAnalyticsEnvelopeMapperTest {

    @Test
    fun `should map analytics turn data to envelope with enriched data`() {
        val turnData = createTurnData()

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(turnData)

        assertEquals("1.0.0", envelope.version)
        assertEquals("envelope-123", envelope.id)
        assertEquals(1705849200L, envelope.date)

        val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelope.data!!)
        assertEquals("test-session-id", parsed.sessionId)
        assertEquals("test-conversation-id", parsed.conversationId)
        assertEquals("request-456", parsed.requestId)
        assertEquals("test-ucp-id", parsed.ucpId)
        assertEquals("test-block", parsed.block)
        assertEquals("sbol", parsed.channel)
        assertEquals("ucf", parsed.appSource)
        assertEquals("ios", parsed.platform)
        assertEquals("test-agent", parsed.agentName)
        assertEquals("test-ci", parsed.agentCi)
        assertEquals("1.0.0", parsed.dataVersion)
        assertEquals("""{"metric":"value"}""", parsed.data)
    }

    @Test
    fun `should calculate size as byte length of raw data`() {
        val rawData = """{"metric":"value"}"""
        val turnData = createTurnData(data = rawData)

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(turnData)

        val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelope.data!!)
        assertEquals(rawData.toByteArray(Charsets.UTF_8).size.toString(), parsed.size)
    }

    @Test
    fun `should default nullable fields to empty string`() {
        val turnData = createTurnData(
            block = null,
            appSource = null,
            platform = null,
            requestId = null
        )

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(turnData)

        val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelope.data!!)
        assertEquals("", parsed.block)
        assertEquals("", parsed.appSource)
        assertEquals("", parsed.platform)
        assertEquals("", parsed.requestId)
    }

    private fun createTurnData(
        block: String? = "test-block",
        appSource: String? = "ucf",
        platform: String? = "ios",
        requestId: String? = "request-456",
        data: String = """{"metric":"value"}"""
    ): AnalyticsTurnData = AnalyticsTurnData(
        envelopeId = "envelope-123",
        timestamp = 1705849200L,
        daSessionInfo = DaSessionInfo(
            meta = DaSessionMeta(
                sessionId = "test-session-id",
                userId = "test-user-id",
                ucpId = "test-ucp-id",
                ufsHost = "test-host"
            ),
            common = DaSessionCommon(
                block = block,
                channel = "sbol",
                appSource = appSource,
                platform = platform
            ),
            userInfo = DaSessionUserInfo()
        ),
        conversationId = "test-conversation-id",
        requestId = requestId,
        agentName = "test-agent",
        agentCi = "test-ci",
        dataVersion = "1.0.0",
        data = data
    )
}
