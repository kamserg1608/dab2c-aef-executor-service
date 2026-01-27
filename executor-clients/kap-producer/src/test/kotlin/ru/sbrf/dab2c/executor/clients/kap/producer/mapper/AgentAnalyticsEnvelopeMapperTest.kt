package ru.sbrf.dab2c.executor.clients.kap.producer.mapper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AgentAnalyticsEnvelopeMapperTest {

    @Test
    fun `should map analytics data to AgentAnalyticsEnvelope with all fields`() {
        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(
            envelopeId = "envelope-123",
            timestamp = 1705849200L,
            data = """{"metric":"value"}"""
        )

        assertEquals("1.2.0", envelope.version)
        assertEquals("envelope-123", envelope.id)
        assertEquals(1705849200L, envelope.date)
        assertEquals("""{"metric":"value"}""", envelope.data)
    }

    @Test
    fun `should pass data string directly to envelope`() {
        val analyticsData = """{"metric1":"value1","count":42}"""

        val envelope = AgentAnalyticsEnvelopeMapper.toAgentAnalyticsEnvelope(
            envelopeId = "test-id",
            timestamp = 1705849200L,
            data = analyticsData
        )

        assertEquals("""{"metric1":"value1","count":42}""", envelope.data)
    }
}
