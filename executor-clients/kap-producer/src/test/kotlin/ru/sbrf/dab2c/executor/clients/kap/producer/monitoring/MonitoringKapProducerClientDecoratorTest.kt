package ru.sbrf.dab2c.executor.clients.kap.producer.monitoring

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.configuration.properties.KapProducerConfigurationProperties
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.UserMessage
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory

class MonitoringKapProducerClientDecoratorTest {

    private val delegate: KapProducerClient = mockk()
    private val metricFactory: MetricFactory = mockk(relaxed = true)
    private val properties: KapProducerConfigurationProperties = mockk()

    private lateinit var decorator: MonitoringKapProducerClientDecorator

    @BeforeEach
    fun setUp() {
        every { properties.dialogsTopic } returns DIALOGS_TOPIC
        every { properties.agentsTopic } returns AGENTS_TOPIC

        decorator = MonitoringKapProducerClientDecorator(
            delegate = delegate,
            properties = properties,
            metricFactory = metricFactory
        )
    }

    @Test
    fun `should publish dialog and record success metrics`() = runTest {
        val dialog = dialogEnvelope()
        val baseTags = dialogBaseTags()
        val successTags = baseTags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS)

        coEvery { delegate.publishDialog(dialog) } returns Unit

        decorator.publishDialog(dialog)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = baseTags
            )
        }
        coVerify(exactly = 1) {
            delegate.publishDialog(dialog)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = successTags
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = successTags
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should publish agent analytics and record success metrics`() = runTest {
        val analytics = agentAnalyticsEnvelope()
        val baseTags = agentAnalyticsBaseTags()
        val successTags = baseTags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS)

        coEvery { delegate.publishAgentAnalytics(analytics) } returns Unit

        decorator.publishAgentAnalytics(analytics)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = baseTags
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = successTags
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = successTags
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should record exception metrics and rethrow on dialog publish error`() = runTest {
        val dialog = dialogEnvelope()
        val exception = IllegalStateException("dialog failed")
        val baseTags = dialogBaseTags()
        val exceptionCounterTags = baseTags + mapOf(
            KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION,
            KapProducerMetricTags.EXCEPTION to IllegalStateException::class.simpleName.orEmpty()
        )
        val exceptionDurationTags = baseTags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION)

        coEvery { delegate.publishDialog(dialog) } throws exception

        val thrown = catchThrowable {
            decorator.publishDialog(dialog)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = baseTags
            )
        }
        coVerify(exactly = 1) {
            delegate.publishDialog(dialog)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
                tags = exceptionCounterTags
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = exceptionDurationTags
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should record exception metrics and rethrow on agent analytics publish error`() = runTest {
        val analytics = agentAnalyticsEnvelope()
        val exception = IllegalArgumentException("analytics failed")
        val baseTags = agentAnalyticsBaseTags()
        val exceptionCounterTags = baseTags + mapOf(
            KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION,
            KapProducerMetricTags.EXCEPTION to IllegalArgumentException::class.simpleName.orEmpty()
        )
        val exceptionDurationTags = baseTags + (KapProducerMetricTags.STATUS to KapProducerMetricTags.EXCEPTION)

        coEvery { delegate.publishAgentAnalytics(analytics) } throws exception

        val thrown = catchThrowable {
            decorator.publishAgentAnalytics(analytics)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = baseTags
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
                tags = exceptionCounterTags
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = exceptionDurationTags
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should rethrow cancellation and not record exception metrics`() = runTest {
        val analytics = agentAnalyticsEnvelope()
        val exception = CancellationException("cancelled")
        val baseTags = agentAnalyticsBaseTags()

        coEvery { delegate.publishAgentAnalytics(analytics) } throws exception

        val thrown = catchThrowable {
            decorator.publishAgentAnalytics(analytics)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = baseTags
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 0) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
                tags = any()
            )
        }
        coVerify(exactly = 0) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = any()
            )
        }
        coVerify(exactly = 0) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = any()
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    private suspend fun catchThrowable(block: suspend () -> Unit): Throwable? =
        try {
            block()
            null
        } catch (e: Throwable) {
            e
        }

    private fun dialogBaseTags(): Map<String, String> =
        mapOf(
            KapProducerMetricTags.MESSAGE_TYPE to KapProducerMetricTags.DIALOG,
            KapProducerMetricTags.TOPIC to DIALOGS_TOPIC
        )

    private fun agentAnalyticsBaseTags(): Map<String, String> =
        mapOf(
            KapProducerMetricTags.MESSAGE_TYPE to KapProducerMetricTags.AGENT_ANALYTICS,
            KapProducerMetricTags.TOPIC to AGENTS_TOPIC
        )

    private fun agentAnalyticsEnvelope(): AgentAnalyticsEnvelope =
        AgentAnalyticsEnvelope(
            version = "1.0",
            id = "analytics-id",
            date = 1_000L,
            data = """{"message_id":"assistant-message-id"}"""
        )

    private fun dialogEnvelope(): DialogEnvelope =
        DialogEnvelope(
            id = "dialog-id",
            version = "1.0",
            date = 1_000L,
            data = DialogData(
                userMessage = UserMessage(
                    chatId = "chat-id",
                    id = "user-message-id",
                    dateCreated = 1_000L,
                    text = "hello"
                )
            )
        )

    private companion object {
        const val DIALOGS_TOPIC = "dialogs-topic"
        const val AGENTS_TOPIC = "agents-topic"
    }
}
