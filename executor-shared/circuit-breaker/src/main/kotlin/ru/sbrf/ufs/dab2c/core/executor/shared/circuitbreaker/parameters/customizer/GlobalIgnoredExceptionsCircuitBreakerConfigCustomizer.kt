package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer

import org.springframework.core.Ordered
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig

/** Default Ignored Exceptions implementation of [CircuitBreakerConfigCustomizer]. */
class GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer(
    private val ignored: Set<Class<out Throwable>> = HashSet()
) : CircuitBreakerConfigCustomizer {

    override fun customize(config: PmsCircuitBreakerConfig) { config.ignoredExceptions += ignored }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE / 2
}
