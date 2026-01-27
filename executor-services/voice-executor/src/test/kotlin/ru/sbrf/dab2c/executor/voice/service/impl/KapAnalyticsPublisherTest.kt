package ru.sbrf.dab2c.executor.voice.service.impl

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics

class KapAnalyticsPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var publisher: KapAnalyticsPublisher

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishAgentAnalytics(any()) } returns Unit
        publisher = KapAnalyticsPublisher(kapProducerClient)
    }

    @Test
    fun `should publish analytics to KAP`() = runTest {
        val analytics = listOf(createTestAnalytics())

        publisher.publishAnalytics(analytics, "request-123")

        coVerify(exactly = 1) { kapProducerClient.publishAgentAnalytics(any()) }
    }

    @Test
    fun `should publish each analytics item separately`() = runTest {
        val analytics = listOf(
            createTestAnalytics(dataVersion = "1.0"),
            createTestAnalytics(dataVersion = "2.0")
        )

        publisher.publishAnalytics(analytics, "request-123")

        coVerify(exactly = 2) { kapProducerClient.publishAgentAnalytics(any()) }
    }

    @Test
    fun `should not publish when analytics list is empty`() = runTest {
        publisher.publishAnalytics(emptyList(), "request-123")

        coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
    }

    @Test
    fun `should set correct version`() = runTest {
        val envelopeSlot = slot<AgentAnalyticsEnvelope>()
        coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

        publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

        assertEquals("1.2.0", envelopeSlot.captured.version)
    }

    @Test
    fun `should pass data string directly to envelope`() = runTest {
        val envelopeSlot = slot<AgentAnalyticsEnvelope>()
        coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit
        val analyticsData = """{"metric":"value","count":42}"""

        publisher.publishAnalytics(listOf(createTestAnalytics(data = analyticsData)), null)

        assertNotNull(envelopeSlot.captured.data)
        assertEquals("""{"metric":"value","count":42}""", envelopeSlot.captured.data)
    }

    @Test
    fun `should generate unique envelope id`() = runTest {
        val envelopes = mutableListOf<AgentAnalyticsEnvelope>()
        coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopes)) } returns Unit

        publisher.publishAnalytics(listOf(createTestAnalytics()), null)
        publisher.publishAnalytics(listOf(createTestAnalytics()), null)

        val ids = envelopes.map { it.id }
        assertEquals(2, ids.distinct().size)
    }

    private fun createTestAnalytics(
        dataVersion: String = "1.0.0",
        data: String = """{"test":"data"}"""
    ): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = data
    )
}
