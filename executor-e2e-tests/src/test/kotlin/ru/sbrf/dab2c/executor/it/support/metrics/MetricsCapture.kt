package ru.sbrf.dab2c.executor.it.support.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Before/after metrics snapshot for integration test assertions.
 * Eliminates per-assertion HTTP calls by caching parsed snapshots.
 */
internal class MetricsCapture(
    private val before: PrometheusMetricsParser,
    private val after: PrometheusMetricsParser,
    private val baseTags: Map<String, String>
) {

    private val logger = KotlinLogging.logger { }

    fun assertCounterIncreased(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap()
    ): MetricsCapture {
        val allTags = baseTags + additionalTags
        val beforeValue = before.findMetric(metricName, allTags)?.value ?: 0.0
        val afterValue = after.findMetric(metricName, allTags)?.value

        val availableInfo = after.findAllMetricsByName(metricName)
            .joinToString("\n") { "  $it" }

        assertThat(afterValue)
            .withFailMessage(
                "Counter '$metricName' should have increased with tags $allTags.\n" +
                    "Before: $beforeValue, After: $afterValue\n" +
                    "Available metrics with this name:\n$availableInfo"
            )
            .isNotNull()
            .isGreaterThan(beforeValue)

        logger.debug { "Counter '$metricName' increased: $beforeValue -> $afterValue (tags: $allTags)" }
        return this
    }

    fun assertTimerRecorded(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap()
    ): MetricsCapture {
        val sumMetric = "${metricName}_sum"
        val allTags = baseTags + additionalTags
        val sumValue = after.findMetric(sumMetric, allTags)?.value

        val availableInfo = after.findAllMetricsByName(sumMetric)
            .joinToString("\n") { "  $it" }

        assertThat(sumValue)
            .withFailMessage(
                "Timer '$metricName' sum should be > 0.0, got: $sumValue\n" +
                    "Available metrics with name '$sumMetric':\n$availableInfo"
            )
            .isNotNull()
            .isGreaterThan(0.0)

        logger.debug { "Timer '$metricName' recorded: sum=$sumValue (tags: $allTags)" }
        return this
    }

    fun assertCounterIncreasedBy(
        metricName: String,
        expectedAmount: Double,
        additionalTags: Map<String, String> = emptyMap()
    ): MetricsCapture {
        val allTags = baseTags + additionalTags
        val beforeValue = before.findMetric(metricName, allTags)?.value ?: 0.0
        val afterValue = after.findMetric(metricName, allTags)?.value

        val availableInfo = after.findAllMetricsByName(metricName)
            .joinToString("\n") { "  $it" }

        assertThat(afterValue)
            .withFailMessage(
                "Counter '$metricName' should have increased by $expectedAmount with tags $allTags.\n" +
                    "Before: $beforeValue, After: $afterValue, Expected delta: $expectedAmount\n" +
                    "Available metrics with this name:\n$availableInfo"
            )
            .isNotNull()

        assertThat(afterValue!! - beforeValue)
            .withFailMessage(
                "Counter '$metricName' delta mismatch.\n" +
                    "Before: $beforeValue, After: $afterValue\n" +
                    "Expected delta: $expectedAmount, Actual delta: ${afterValue - beforeValue}"
            )
            .isEqualTo(expectedAmount)

        logger.debug {
            "Counter '$metricName' increased by $expectedAmount: $beforeValue -> $afterValue (tags: $allTags)"
        }
        return this
    }

    fun assertCounterNotIncreased(
        metricName: String,
        additionalTags: Map<String, String> = emptyMap()
    ): MetricsCapture {
        val allTags = baseTags + additionalTags
        val beforeValue = before.findMetric(metricName, allTags)?.value ?: 0.0
        val afterValue = after.findMetric(metricName, allTags)?.value ?: 0.0

        assertThat(afterValue)
            .withFailMessage(
                "Counter '$metricName' should NOT have increased with tags $allTags.\n" +
                    "Before: $beforeValue, After: $afterValue"
            )
            .isEqualTo(beforeValue)

        return this
    }

    fun assertGaugeEquals(
        metricName: String,
        expected: Double,
        additionalTags: Map<String, String> = emptyMap()
    ): MetricsCapture {
        val allTags = baseTags + additionalTags
        val actual = after.findMetric(metricName, allTags)?.value

        assertThat(actual)
            .withFailMessage(
                "Gauge '$metricName' expected=$expected, got: $actual (tags: $allTags)"
            )
            .isEqualTo(expected)

        return this
    }

    companion object {
        private val logger = KotlinLogging.logger { }

        suspend fun fetchAndParse(httpClient: HttpClient): PrometheusMetricsParser {
            val body = httpClient.get("/actuator/metrics") {
                header("Accept", "text/plain; version=0.0.4; charset=utf-8")
            }.bodyAsText()
            logger.debug { "Fetched metrics snapshot (${body.lines().size} lines)" }
            return PrometheusMetricsParser(body)
        }
    }
}

/**
 * Takes a metrics snapshot before and after [block], returning a [MetricsCapture] for assertions.
 */
internal suspend fun captureMetrics(
    httpClient: HttpClient,
    baseTags: Map<String, String> = emptyMap(),
    block: suspend () -> Unit
): MetricsCapture {
    val before = MetricsCapture.fetchAndParse(httpClient)
    block()
    val after = MetricsCapture.fetchAndParse(httpClient)
    return MetricsCapture(before, after, baseTags)
}

/**
 * Like [captureMetrics], but keeps re-reading the after-snapshot until the named counter grows past
 * its pre-block value. For effects that land after [block] returns, such as a cancelled call.
 */
@Suppress("LongParameterList")
internal suspend fun captureMetricsAwaitingCounter(
    httpClient: HttpClient,
    baseTags: Map<String, String>,
    metricName: String,
    counterTags: Map<String, String>,
    timeout: Duration = 10.seconds,
    pollInterval: Duration = 200.milliseconds,
    block: suspend () -> Unit
): MetricsCapture {
    val before = MetricsCapture.fetchAndParse(httpClient)
    block()

    val allTags = baseTags + counterTags
    val beforeValue = before.findMetric(metricName, allTags)?.value ?: 0.0
    var after = MetricsCapture.fetchAndParse(httpClient)
    fun currentValue(): Double = after.findMetric(metricName, allTags)?.value ?: 0.0

    withTimeoutOrNull(timeout) {
        while (currentValue() <= beforeValue) {
            delay(pollInterval)
            after = MetricsCapture.fetchAndParse(httpClient)
        }
    }
    return MetricsCapture(before, after, baseTags)
}
