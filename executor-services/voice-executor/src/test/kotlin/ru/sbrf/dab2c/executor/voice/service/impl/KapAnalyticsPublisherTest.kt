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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.voice.grpc.context.MetadataElement
import ru.sbrf.dab2c.executor.voice.model.ProcessingState
import ru.sbrf.dab2c.executor.voice.model.RequestMetadata

class KapAnalyticsPublisherTest {

    private lateinit var kapProducerClient: KapProducerClient
    private lateinit var processingState: MutableStateFlow<ProcessingState>
    private lateinit var publisher: KapAnalyticsPublisher
    private lateinit var metadataContext: MetadataElement

    @BeforeEach
    fun setUp() {
        kapProducerClient = mockk()
        coEvery { kapProducerClient.publishAgentAnalytics(any()) } returns Unit
        processingState = MutableStateFlow(createServingState())
        publisher = KapAnalyticsPublisher(kapProducerClient, processingState)

        val metadata = RequestMetadata(emptyMap())
        metadata._daSessionInfo = createDaSessionInfo()
        metadataContext = MetadataElement(metadata)
    }

    @Test
    fun `should publish analytics to KAP`() = runTest {
        withContext(metadataContext) {
            val analytics = listOf(createTestAnalytics())

            publisher.publishAnalytics(analytics, "request-123")

            coVerify(exactly = 1) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should publish each analytics item separately`() = runTest {
        withContext(metadataContext) {
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
        withContext(metadataContext) {
            publisher.publishAnalytics(emptyList(), "request-123")

            coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should not publish when not in Serving state`() = runTest {
        withContext(metadataContext) {
            processingState.value = ProcessingState.AwaitingContext
            val analytics = listOf(createTestAnalytics())

            publisher.publishAnalytics(analytics, "request-123")

            coVerify(exactly = 0) { kapProducerClient.publishAgentAnalytics(any()) }
        }
    }

    @Test
    fun `should set correct version`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

            assertEquals("1.1.0", envelopeSlot.captured.version)
        }
    }

    @Test
    fun `should set session info from context`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

            val captured = envelopeSlot.captured
            assertEquals("test-session-id", captured.sessionId)
            assertEquals("test-conversation-id", captured.conversationId)
            assertEquals("test-ucp-id", captured.ucpId)
            assertEquals("test-block", captured.block)
            assertEquals("sbol", captured.channel)
        }
    }

    @Test
    fun `should set agent info from configuration`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-123")

            val captured = envelopeSlot.captured
            assertEquals("test-agent", captured.agentName)
            assertEquals("test-ci", captured.agentCi)
        }
    }

    @Test
    fun `should set data version from analytics`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics(dataVersion = "2.5.0")), "request-123")

            assertEquals("2.5.0", envelopeSlot.captured.dataVersion)
        }
    }

    @Test
    fun `should pass data string directly to envelope`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit
            val analyticsData = """{"metric":"value","count":42}"""

            publisher.publishAnalytics(listOf(createTestAnalytics(data = analyticsData)), null)

            assertNotNull(envelopeSlot.captured.data)
            assertEquals("""{"metric":"value","count":42}""", envelopeSlot.captured.data)
        }
    }

    @Test
    fun `should include requestId when provided`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), "request-456")

            assertEquals("request-456", envelopeSlot.captured.requestId)
        }
    }

    @Test
    fun `should handle null requestId`() = runTest {
        withContext(metadataContext) {
            val envelopeSlot = slot<AgentAnalyticsEnvelope>()
            coEvery { kapProducerClient.publishAgentAnalytics(capture(envelopeSlot)) } returns Unit

            publisher.publishAnalytics(listOf(createTestAnalytics()), null)

            assertNull(envelopeSlot.captured.requestId)
        }
    }

    @Test
    fun `should generate unique envelope id`() = runTest {
        withContext(metadataContext) {
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
