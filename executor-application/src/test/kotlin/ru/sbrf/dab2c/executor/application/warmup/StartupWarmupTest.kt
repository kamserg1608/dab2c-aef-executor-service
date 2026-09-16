package ru.sbrf.dab2c.executor.application.warmup

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

/** Verifies that a failing warmup still lets startup proceed. */
class StartupWarmupTest {

    private val httpClient = HttpClient(CIO)

    @AfterEach
    fun closeHttpClient() {
        httpClient.close()
    }

    @Test
    fun `start completes normally when the warmup pass fails`() {
        val warmup = StartupWarmup(mapOf("warmupHttpClient" to httpClient), WarmupProperties())

        warmup.start()

        assertThat(warmup.isRunning()).isTrue()
    }
}
