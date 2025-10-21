package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer

import org.springframework.core.Ordered
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig

/**
 * Interface for [PmsCircuitBreakerConfig] customizers.
 */
interface CircuitBreakerConfigCustomizer : Ordered {

    /**
     * Can customize [PmsCircuitBreakerConfig].
     */
    fun customize(config: PmsCircuitBreakerConfig)

    /**
     * Returns true, if this customizer can customize config for given service name or else false.
     */
    fun supportsService(serviceName: String): Boolean = true
}
