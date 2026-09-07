package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

class KapAnalyticsPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var publisher: KapAnalyticsPublisher
    private lateinit var headersElement: HeadersElement
    private lateinit var sessionInfoElement: SessionInfoElement

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishAgentAnalytics(any()) } returns Unit
        publisher = KapAnalyticsPublisher(kapProducerClient)

        headersElement = HeadersElement(Headers(emptyMap()))
        sessionInfoElement = SessionInfoElement(createDaSessionInfo())
    }

    @Test
    fun `should publish analytics to KAP`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            publish(listOf(createTestAnalytics()), "request-123")

            coVerify(exactly = 1) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should publish each analytics item separately`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val analytics = listOf(
                createTestAnalytics(dataVersion = "1.0"),
                createTestAnalytics(dataVersion = "2.0")
            )

            publish(analytics, "request-123")

            coVerify(exactly = 2) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should not publish when analytics list is empty`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            publish(emptyList(), "request-123")

            coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should set correct version`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publish(listOf(createTestAnalytics()), "request-123")

            assertEquals("1.0.0", envelopeSlot.captured.version)
        }
    }

    @Test
    fun `should enrich data with session context`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit
            val analyticsData = """{"metric":"value","count":42}"""

            publish(listOf(createTestAnalytics(data = analyticsData)), "request-123")

            assertNotNull(envelopeSlot.captured.data)
            val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelopeSlot.captured.data!!)
            assertEquals("test-session-id", parsed.sessionId)
            assertEquals("test-conversation-id", parsed.conversationId)
            assertEquals("request-123", parsed.requestId)
            assertEquals(ASSISTANT_MESSAGE_ID, parsed.messageId)
            assertEquals("test-ucp-id", parsed.ucpId)
            assertEquals("test-block", parsed.block)
            assertEquals("sbol", parsed.channel)
            assertEquals("ucf", parsed.appSource)
            assertEquals("ios", parsed.platform)
            assertEquals("test-agent", parsed.agentName)
            assertEquals("test-ci", parsed.agentCi)
            assertEquals("1.0.0", parsed.dataVersion)
            assertEquals(analyticsData, parsed.data)
        }
    }

    @Test
    fun `should generate unique envelope id`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val envelopes = mutableListOf<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopes)) } returns Unit

            publish(listOf(createTestAnalytics()), null)
            publish(listOf(createTestAnalytics()), null)

            val ids = envelopes.map { it.id }
            assertEquals(2, ids.distinct().size)
        }
    }

    @Test
    fun `should fall back to the x-request-id header when no request id is given`() = runTest {
        val envelopeSlot = slot<AgentAnalyticsEnvelope>()
        coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit
        val headers = HeadersElement(Headers(mapOf("x-request-id" to "header-request-id")))

        withContext(headers + sessionInfoElement) {
            publish(listOf(createTestAnalytics()), null)
        }

        val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelopeSlot.captured.data!!)
        assertEquals("header-request-id", parsed.requestId)
    }

    private suspend fun publish(analytics: List<AgentAnalytics>, requestId: String?) =
        publisher.publishAnalytics(
            analytics = analytics,
            requestId = requestId,
            conversationId = "test-conversation-id",
            agentConfiguration = agentConfiguration(),
            assistantMessageId = ASSISTANT_MESSAGE_ID
        )

    private fun agentConfiguration(): AgentConfiguration = AgentConfiguration(
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
    )

    private fun createTestAnalytics(
        dataVersion: String = "1.0.0",
        data: String = """{"test":"data"}"""
    ): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = data
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
            appSource = "ucf",
            platform = "ios"
        ),
        userInfo = DaSessionUserInfo()
    )

    private companion object {
        const val ASSISTANT_MESSAGE_ID = "test-assistant-message-id"
    }
}
