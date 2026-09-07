package ru.sbrf.dab2c.executor.clients.kap.producer.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope

private val logger = KotlinLogging.logger {}

/**
 * Decorator that offloads KapProducerClient operations to a background coroutine on IO dispatcher.
 * Returns immediately without blocking the caller.
 */
class AsyncKapProducerClientDecorator(
    private val delegate: KapProducerClient,
    private val scope: CoroutineScope
) : KapProducerClient {

    override suspend fun publishDialog(dialog: DialogEnvelope) {
        scope.launch(Dispatchers.IO) {
            try {
                delegate.publishDialog(dialog)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to publish dialog id=${dialog.id}" }
            }
        }
    }

    override suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope) {
        scope.launch(Dispatchers.IO) {
            try {
                delegate.publishAgentAnalytics(analytics)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to publish agent analytics id=${analytics.id}" }
            }
        }
    }
}
