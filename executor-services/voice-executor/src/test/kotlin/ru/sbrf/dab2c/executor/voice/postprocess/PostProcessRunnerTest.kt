package ru.sbrf.dab2c.executor.voice.postprocess

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessResult
import ru.sbrf.dab2c.executor.domain.voice.AgentAnalytics
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.CONVERSATION_ID
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.agentConfiguration
import ru.sbrf.dab2c.executor.voice.service.impl.DialogContextTestFixtures.dialogContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Verifies that every submitted snapshot reaches the agent, that the returned analytics are
 * published, and that a failing call is swallowed while cancellation still propagates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PostProcessRunnerTest {

    private val client: GigaVoiceAgentClient = mockk()
    private val analyticsPublisher: AnalyticsPublisher = mockk(relaxed = true)
    private val scopeJob: Job = SupervisorJob()
    private val snapshot = PostProcessSnapshot(
        CONVERSATION_ID, agentConfiguration, dialogContext("""{"a":1}"""), ASSISTANT_MESSAGE_ID
    )

    @Test
    fun `should send every submitted snapshot to the agent`() = runTest {
        coEvery { client.postProcess(any(), any(), any()) } returns PostProcessResult()
        val runner = runner()

        repeat(SUBMISSIONS) { runner.submit(snapshot, EmptyCoroutineContext) }
        advanceUntilIdle()

        coVerify(exactly = SUBMISSIONS) {
            client.postProcess(CONVERSATION_ID, agentConfiguration, snapshot.contextData)
        }
    }

    @Test
    fun `should publish the analytics the agent returned`() = runTest {
        val analytics = listOf(AgentAnalytics(dataVersion = "1.0.0", data = """{"a":1}"""))
        coEvery { client.postProcess(any(), any(), any()) } returns PostProcessResult(analytics = analytics)
        val runner = runner()

        runner.submit(snapshot, EmptyCoroutineContext)
        advanceUntilIdle()

        coVerify {
            analyticsPublisher.publishAnalytics(
                analytics = analytics,
                requestId = null,
                conversationId = CONVERSATION_ID,
                agentConfiguration = agentConfiguration,
                assistantMessageId = ASSISTANT_MESSAGE_ID
            )
        }
    }

    @Test
    fun `should keep the task alive when publishing fails`() = runTest {
        coEvery { client.postProcess(any(), any(), any()) } returns PostProcessResult()
        coEvery { analyticsPublisher.publishAnalytics(any(), any(), any(), any(), any()) } throws
            IllegalStateException("KAP is down")
        val runner = runner()

        runner.submit(snapshot, EmptyCoroutineContext)
        val task = scopeJob.children.single()
        advanceUntilIdle()

        assertThat(task.isCancelled).isFalse()
        assertThat(task.isCompleted).isTrue()
    }

    @Test
    fun `should cancel the task on cancellation but swallow an integration failure`() = runTest {
        coEvery { client.postProcess(any(), any(), any()) } throws CancellationException("scope is going down")
        val runner = runner()

        runner.submit(snapshot, EmptyCoroutineContext)
        val cancelled = scopeJob.children.single()
        advanceUntilIdle()

        coEvery { client.postProcess(any(), any(), any()) } throws IllegalStateException("agent is down")
        runner.submit(snapshot, EmptyCoroutineContext)
        val failed = scopeJob.children.single()
        advanceUntilIdle()

        assertThat(cancelled.isCancelled).isTrue()
        assertThat(failed.isCancelled).isFalse()
        assertThat(failed.isCompleted).isTrue()
    }

    private fun TestScope.runner() = PostProcessRunner(
        scope = CoroutineScope(scopeJob + StandardTestDispatcher(testScheduler)),
        gigaVoiceAgentClient = client,
        analyticsPublisher = analyticsPublisher
    )

    private companion object {
        const val SUBMISSIONS = 50
        const val ASSISTANT_MESSAGE_ID = "test-assistant-message-id"
    }
}
