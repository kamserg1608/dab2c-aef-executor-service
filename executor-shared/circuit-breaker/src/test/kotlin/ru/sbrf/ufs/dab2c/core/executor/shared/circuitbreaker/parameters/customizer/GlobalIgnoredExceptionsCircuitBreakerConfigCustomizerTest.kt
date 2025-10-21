package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.core.Ordered
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import java.util.concurrent.TimeoutException

class GlobalIgnoredExceptionsCircuitBreakerConfigCustomizerTest {

    private val ignoredExceptions = setOf(
        IllegalStateException::class.java,
        TimeoutException::class.java,
    )

    private val customizer = GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer(ignoredExceptions)

    @Test
    fun `test customize should add ignoredExceptions to set of predefinedExceptions`() {
        val predefinedExceptions = setOf(IllegalAccessException::class.java)

        val config = PmsCircuitBreakerConfig(
            serviceName = "service-name",
            ignoredExceptions = predefinedExceptions,
        )

        customizer.customize(config)

        Assertions.assertEquals(config.ignoredExceptions, predefinedExceptions + ignoredExceptions)
    }

    @Test
    fun `test order should return proper value`() {
        Assertions.assertEquals(customizer.order, Ordered.LOWEST_PRECEDENCE / 2)
    }

    @Test
    fun `test supportService should support any service`() {
        Assertions.assertTrue(customizer.supportsService("service-name"))
    }
}
