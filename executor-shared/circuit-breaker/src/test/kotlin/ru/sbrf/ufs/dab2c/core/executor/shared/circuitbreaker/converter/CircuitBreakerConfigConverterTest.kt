package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import java.time.Duration

class CircuitBreakerConfigConverterTest {

    @Test
    fun `test convert should make full conversion`() {
        val objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        val config = PmsCircuitBreakerConfig(
            serviceName = "service-name",
            isEnabled = true,
            failureRateThreshold = 50.0F,
            recordedExceptions = setOf(IllegalStateException::class.java),
            ignoredExceptions = setOf(IllegalArgumentException::class.java),
            permittedNumberOfCallsInHalfOpenState = 10,
            slidingWindow = 100,
            waitInOpenStateDuration = Duration.parse("PT1M")
        )

        val result = CircuitBreakerConfigConverter.convert(config)

        val expected = CircuitBreakerConfig.custom()
            .writableStackTraceEnabled(false)
            .failureRateThreshold(50.0F)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .ringBufferSizeInClosedState(100)
            .ringBufferSizeInHalfOpenState(10)
            .waitDurationInOpenState(Duration.parse("PT1M"))
            .recordExceptions(*arrayOf(IllegalStateException::class.java))
            .ignoreExceptions(*arrayOf(IllegalArgumentException::class.java))
            .recordFailure { false }
            .build()
        Assertions.assertEquals(objectMapper.writeValueAsString(expected), objectMapper.writeValueAsString(result))
    }
}
