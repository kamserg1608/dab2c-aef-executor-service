package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer

import org.springframework.core.Ordered
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig

/** Default Recorded Exceptions implementation of [CircuitBreakerConfigCustomizer]. */
class GlobalRecordedExceptionsCircuitBreakerConfigCustomizer(
    private val recorded: Set<Class<out Throwable>> = setOf(Exception::class.java)
) : CircuitBreakerConfigCustomizer {

    override fun customize(config: PmsCircuitBreakerConfig) { config.recordedExceptions += recorded }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE / 2
}
