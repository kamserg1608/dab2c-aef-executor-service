package ru.sbrf.dab2c.executor.clients.giga.agent.monitoring

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.common.model.ClientMetric
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.RequestBuilderFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricTags

/**
 * Verifies that postProcess is counted as an HTTP integration call to the agent.
 */
class MonitoringGigaVoiceAgentClientDecoratorTest {

    private val delegate: GigaVoiceAgentClient = mockk()
    private val metricFactory: MetricFactory = mockk(relaxed = true)
    private val decorator = MonitoringGigaVoiceAgentClientDecorator(delegate, metricFactory)

    @Test
    fun `should count postProcess against the agent endpoint and return the delegate result`() = runTest {
        val expected = PostProcessResult(analytics = emptyList())
        val contextData = DialogContext(ObjectMappers.MAPPER.createObjectNode())
        coEvery { delegate.postProcess("conv-1", agentConfiguration, contextData) } returns expected
        val tags = slot<Map<String, String>>()

        val result = decorator.postProcess("conv-1", agentConfiguration, contextData)

        coVerify {
            metricFactory.incrementCounter(ClientMetric.HTTP_INTEGRATION_REQUEST_TOTAL, capture(tags))
        }
        assertThat(result).isSameAs(expected)
        assertThat(tags.captured).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                MetricTags.DESTINATION_SERVICE to "gigavoice-agent",
                MetricTags.ENDPOINT to "/postprocess",
                MetricTags.METHOD to "post",
                MetricTags.STATUS_CODE to "200"
            )
        )
    }
}
