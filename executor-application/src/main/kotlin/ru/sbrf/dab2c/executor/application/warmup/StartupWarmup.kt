package ru.sbrf.dab2c.executor.application.warmup

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.retry
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.SmartLifecycle
import org.springframework.stereotype.Component
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers.MAPPER
import java.lang.management.ManagementFactory
import kotlin.time.measureTime

private val logger = KotlinLogging.logger {}

private const val JACKSON_STEP = "jackson"
private const val HEALTH_PATH = "/actuator/health/warmup"

/**
 * Runs one synthetic pass over Jackson metamodels and Ktor engines after the HTTP server is up
 * and before the gRPC server starts accepting calls; a failed step never blocks startup.
 */
@Component
@EnableConfigurationProperties(WarmupProperties::class)
class StartupWarmup(
    private val httpClients: Map<String, HttpClient>,
    private val properties: WarmupProperties
) : SmartLifecycle, ApplicationListener<WebServerInitializedEvent> {

    @Volatile
    private var running = false

    @Volatile
    private var serverPort = 0

    override fun onApplicationEvent(event: WebServerInitializedEvent) {
        serverPort = event.webServer.port
    }

    override fun getPhase(): Int = Integer.MAX_VALUE - 1

    override fun isRunning(): Boolean = running

    override fun start() {
        if (!properties.enabled) {
            running = true
            logger.info { "Startup warmup disabled" }
            return
        }

        val classLoading = ManagementFactory.getClassLoadingMXBean()
        val loadedBefore = classLoading.totalLoadedClassCount
        val failed = mutableListOf<String>()
        val elapsed = measureTime { runBlocking { warmUp(failed) } }
        running = true

        val summary = "in $elapsed, classes loaded: ${classLoading.totalLoadedClassCount - loadedBefore}"
        if (failed.isEmpty()) {
            logger.info { "Startup warmup completed $summary" }
        } else {
            logger.warn { "Startup warmup incomplete $summary, not warmed: ${failed.joinToString()}" }
        }
    }

    override fun stop() {
        running = false
    }

    private suspend fun warmUp(failed: MutableList<String>) {
        step(JACKSON_STEP, failed) { warmUpJackson() }
        httpClients.forEach { (name, client) -> step(name, failed) { warmUpEngine(client) } }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun step(name: String, failed: MutableList<String>, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            failed += name
            logger.warn(e) { "Startup warmup step '$name' failed" }
        }
    }

    private fun warmUpJackson() {
        WarmupPayloads.DESERIALIZED_TYPES.forEach { MAPPER.canDeserialize(MAPPER.constructType(it)) }
        WarmupPayloads.SERIALIZED_PAYLOADS.forEach { MAPPER.writeValueAsString(it) }
    }

    private suspend fun warmUpEngine(client: HttpClient) {
        client.get("http://127.0.0.1:$serverPort$HEALTH_PATH") { retry { noRetry() } }
            .body<Map<String, Any>>()
    }
}
