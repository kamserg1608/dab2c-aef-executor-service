package ru.sbrf.dab2c.executor.voice.service.impl

import com.fasterxml.jackson.module.kotlin.readValue
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
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.VoiceSession

class KapAnalyticsPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var session: VoiceSession
    private lateinit var publisher: KapAnalyticsPublisher
    private lateinit var headersElement: HeadersElement
    private lateinit var sessionInfoElement: SessionInfoElement

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishAgentAnalytics(any()) } returns Unit
        session = VoiceSession(state = MutableStateFlow(createServingState()))
        publisher = KapAnalyticsPublisher(kapProducerClient, session)

        headersElement = HeadersElement(Headers(emptyMap()))
        sessionInfoElement = SessionInfoElement(createDaSessionInfo())
    }

    @Test
    fun `should publish analytics to KAP`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

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

            publisher.publishAnalytics(analytics, "request-123")

            coVerify(exactly = 2) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should not publish when analytics list is empty`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            publisher.publishAnalytics(emptyList(), "request-123")

            coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should not publish when not in Serving state`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            session.state.value = ProcessingState.AwaitingContext

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

            coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should set correct version`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

            assertEquals("1.0.0", envelopeSlot.captured.version)
        }
    }

    @Test
    fun `should enrich data with session context`() = runTest {
        withContext(headersElement + sessionInfoElement) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit
            val analyticsData = """{"metric":"value","count":42}"""

            publisher.publishAnalytics(listOf(createTestAnalytics(data = analyticsData)), "request-123")

            assertNotNull(envelopeSlot.captured.data)
            val parsed = ObjectMappers.MAPPER.readValue<AgentAnalyticsData>(envelopeSlot.captured.data!!)
            assertEquals("test-session-id", parsed.sessionId)
            assertEquals("test-conversation-id", parsed.conversationId)
            assertEquals("request-123", parsed.requestId)
            assertEquals(session.turnIds.assistantMessageId, parsed.messageId)
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

            publisher.publishAnalytics(listOf(createTestAnalytics()), null)
            publisher.publishAnalytics(listOf(createTestAnalytics()), null)

            val ids = envelopes.map { it.id }
            assertEquals(2, ids.distinct().size)
        }
    }

    private fun createTestAnalytics(
        dataVersion: String = "1.0.0",
        data: String = """{"test":"data"}"""
    ): AgentAnalytics = AgentAnalytics(
        dataVersion = dataVersion,
        data = data
    )

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
            appSource = "ucf",
            platform = "ios"
        ),
        userInfo = DaSessionUserInfo()
    )
}
