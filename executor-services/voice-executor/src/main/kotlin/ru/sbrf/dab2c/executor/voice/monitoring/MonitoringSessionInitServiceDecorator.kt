package ru.sbrf.dab2c.executor.voice.monitoring

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.monitoring.service.api.MetricFactory
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitResult
import ru.sbrf.dab2c.executor.voice.service.api.SessionInitService

/**
 * Decorator that records gRPC session initialization duration metrics.
 * Delegates to the primary SessionInitService implementation and measures
 * the time spent in session initialization.
 */
@Service
@Primary
class MonitoringSessionInitServiceDecorator(
    @Qualifier("sessionInitServiceImpl") private val delegate: SessionInitService,
    private val metricFactory: MetricFactory,
) : SessionInitService {

    override suspend fun initialize(): SessionInitResult {
        val startTime = System.nanoTime()

        return try {
            val result = delegate.initialize()
            recordDuration(startTime)
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordDuration(startTime)
            throw e
        }
    }

    private suspend fun recordDuration(startTime: Long) {
        metricFactory.recordDuration(
            metric = ExecutorVoiceMetric.GRPC_SESSION_INITIALIZATION_DURATION_SECONDS,
            durationNs = System.nanoTime() - startTime,
            tags = emptyMap()
        )
    }

    @Suppress("LabeledExpression")
    override fun <T> Flow<T>.withSessionContext(headers: Headers): Flow<T> =
        with(delegate) { this@withSessionContext.withSessionContext(headers) }
}
