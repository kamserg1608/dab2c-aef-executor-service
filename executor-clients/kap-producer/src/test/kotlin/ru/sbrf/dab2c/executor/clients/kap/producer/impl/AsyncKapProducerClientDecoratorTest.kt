package ru.sbrf.dab2c.executor.clients.kap.producer.impl

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogData
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.UserMessage

/**
 * The background coroutine ends as cancelled when publishing is cancelled,
 * and completes normally when publishing fails for any other reason.
 */
class AsyncKapProducerClientDecoratorTest {

    private val delegate: KapProducerClient = mockk()
    private val publishing = CompletableDeferred<Unit>()

    @Test
    fun `should end the dialog publish coroutine as cancelled on cancellation`() = runTest {
        coEvery { delegate.publishDialog(DIALOG) } coAnswers {
            publishing.await()
            throw CancellationException("scope is gone")
        }

        val child = publishAndAwaitChild { it.publishDialog(DIALOG) }

        assertThat(child.isCancelled).isTrue()
    }

    @Test
    fun `should end the analytics publish coroutine as cancelled on cancellation`() = runTest {
        coEvery { delegate.publishAgentAnalytics(ANALYTICS) } coAnswers {
            publishing.await()
            throw CancellationException("scope is gone")
        }

        val child = publishAndAwaitChild { it.publishAgentAnalytics(ANALYTICS) }

        assertThat(child.isCancelled).isTrue()
    }

    @Test
    fun `should swallow a publish failure and complete the coroutine normally`() = runTest {
        coEvery { delegate.publishDialog(DIALOG) } coAnswers {
            publishing.await()
            error("kafka is down")
        }

        val child = publishAndAwaitChild { it.publishDialog(DIALOG) }

        assertThat(child.isCancelled).isFalse()
        assertThat(child.isCompleted).isTrue()
    }

    private suspend fun publishAndAwaitChild(publish: suspend (KapProducerClient) -> Unit): Job {
        val scopeJob = SupervisorJob()
        publish(AsyncKapProducerClientDecorator(delegate, CoroutineScope(scopeJob)))

        val child = scopeJob.children.single()
        publishing.complete(Unit)
        child.join()
        scopeJob.cancel()
        return child
    }

    private companion object {
        val ANALYTICS = AgentAnalyticsEnvelope(
            version = "1.0",
            id = "analytics-id",
            date = 1_000L,
            data = """{"message_id":"assistant-message-id"}"""
        )

        val DIALOG = DialogEnvelope(
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
    }
}
