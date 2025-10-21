package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.service.impl

import io.github.resilience4j.circuitbreaker.event.CircuitBreakerEvent
import io.github.resilience4j.core.EventConsumer
import ru.sbrf.ufs.platform.logger.LoggerFactory

/**
 * Logging [EventConsumer].
 */
class LoggingEventConsumerImpl : EventConsumer<CircuitBreakerEvent> {

    private val logger = LoggerFactory.getLogger(LoggingEventConsumerImpl::class.java)

    override fun consumeEvent(event: CircuitBreakerEvent) {
        if (event.eventType == CircuitBreakerEvent.Type.NOT_PERMITTED) {
            logger.warn("Call via [{}] was prohibited [{}]!", event.circuitBreakerName, event)
        } else {
            logger.debug("CircuitBreaker [{}] received event [{}]!", event.circuitBreakerName, event)
        }
    }
}
