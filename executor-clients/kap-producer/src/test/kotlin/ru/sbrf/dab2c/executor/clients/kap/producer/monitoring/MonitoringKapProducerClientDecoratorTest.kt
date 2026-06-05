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

        coEvery { delegate.publishDialog(dialog) } returns Unit

        decorator.publishDialog(dialog)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = preparedTags(DIALOGS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            delegate.publishDialog(dialog)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = publishedTags(DIALOGS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = durationTags(DIALOGS_TOPIC)
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should publish agent analytics and record success metrics`() = runTest {
        val analytics = agentAnalyticsEnvelope()

        coEvery { delegate.publishAgentAnalytics(analytics) } returns Unit

        decorator.publishAgentAnalytics(analytics)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = preparedTags(AGENTS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = publishedTags(AGENTS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = durationTags(AGENTS_TOPIC)
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should record exception metrics and rethrow on dialog publish error`() = runTest {
        val dialog = dialogEnvelope()
        val exception = IllegalStateException("dialog failed")

        coEvery { delegate.publishDialog(dialog) } throws exception

        val thrown = catchThrowable {
            decorator.publishDialog(dialog)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = preparedTags(DIALOGS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            delegate.publishDialog(dialog)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
                tags = exceptionTags(
                    destination = DIALOGS_TOPIC,
                    exceptionType = IllegalStateException::class.simpleName.orEmpty()
                )
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = durationTags(DIALOGS_TOPIC)
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should record exception metrics and rethrow on agent analytics publish error`() = runTest {
        val analytics = agentAnalyticsEnvelope()
        val exception = IllegalArgumentException("analytics failed")

        coEvery { delegate.publishAgentAnalytics(analytics) } throws exception

        val thrown = catchThrowable {
            decorator.publishAgentAnalytics(analytics)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = preparedTags(AGENTS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
                tags = exceptionTags(
                    destination = AGENTS_TOPIC,
                    exceptionType = IllegalArgumentException::class.simpleName.orEmpty()
                )
            )
        }
        coVerify(exactly = 1) {
            metricFactory.recordDuration(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_DURATION_SECONDS,
                durationNs = any(),
                tags = durationTags(AGENTS_TOPIC)
            )
        }

        confirmVerified(delegate, metricFactory)
    }

    @Test
    fun `should rethrow cancellation and not record publish metrics`() = runTest {
        val analytics = agentAnalyticsEnvelope()
        val exception = CancellationException("cancelled")

        coEvery { delegate.publishAgentAnalytics(analytics) } throws exception

        val thrown = catchThrowable {
            decorator.publishAgentAnalytics(analytics)
        }

        assertThat(thrown).isSameAs(exception)

        coVerify(exactly = 1) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PREPARED_MESSAGES_TOTAL,
                tags = preparedTags(AGENTS_TOPIC)
            )
        }
        coVerify(exactly = 1) {
            delegate.publishAgentAnalytics(analytics)
        }
        coVerify(exactly = 0) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_TOTAL,
                tags = any()
            )
        }
        coVerify(exactly = 0) {
            metricFactory.incrementCounter(
                metric = KapProducerMetric.ANALYTICS_PUBLISHED_MESSAGES_EXCEPTIONS_TOTAL,
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

    private fun preparedTags(destination: String): Map<String, String> =
        mapOf(
            KapProducerMetricTags.DESTINATION to destination,
            KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS
        )

    private fun publishedTags(destination: String): Map<String, String> =
        mapOf(
            KapProducerMetricTags.DESTINATION to destination,
            KapProducerMetricTags.STATUS to KapProducerMetricTags.SUCCESS
        )

    private fun exceptionTags(
        destination: String,
        exceptionType: String
    ): Map<String, String> =
        mapOf(
            KapProducerMetricTags.DESTINATION to destination,
            KapProducerMetricTags.EXCEPTION_TYPE to exceptionType
        )

    private fun durationTags(destination: String): Map<String, String> =
        mapOf(KapProducerMetricTags.DESTINATION to destination)

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
        const val DIALOGS_TOPIC = "dab2c-dialogs"
        const val AGENTS_TOPIC = "dab2c-agents"
    }
}
