package ru.sbrf.dab2c.executor.it.tests.grpc

import io.github.oshai.kotlinlogging.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.contextRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceRequestFixtures.settingsRequest
import ru.sbrf.dab2c.executor.it.support.fixtures.GigaVoiceResponseFixtures.outputTranscriptionResponse
import ru.sbrf.dab2c.executor.it.support.metrics.MetricsCapture
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.session.withSession
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.setupStubs
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.voice.model.ExecutorVoiceMetric
import java.lang.management.ManagementFactory

/**
 * Detects gaps in the startup warmup by comparing the first session in the JVM against
 * later ones, which are warm by definition. Must run first, hence [Order].
 */
@Order(1)
class WarmupCoverageTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun `first session loads about as few classes as a warm one`() = runItTest {
        setupStubs(efsAdapterMock, gigaVoiceAgentMock)
        assertNoSessionRanYet()

        val first = classesLoadedBy { runSession("warmup-coverage-first") }
        val warm = (1..WARM_SESSIONS).map { index -> classesLoadedBy { runSession("warmup-coverage-$index") } }
        val baseline = warm.min()
        logger.info { "Warmup coverage: first session loaded $first classes, later sessions $warm" }

        assertThat(first - baseline)
            .withFailMessage(
                "First session loaded $first classes against $baseline for the warmest of " +
                    "$WARM_SESSIONS later sessions, which is above the allowed extra of $MAX_EXTRA_CLASSES. " +
                    "Either the startup warmup is off, or a newly used type is missing from WarmupPayloads."
            )
            .isLessThan(MAX_EXTRA_CLASSES)
    }

    private suspend fun assertNoSessionRanYet() {
        val sessions = MetricsCapture.fetchAndParse(httpClient)
            .findAllMetricsByName(ExecutorVoiceMetric.GRPC_CONNECTIONS_TOTAL.metricName)
            .sumOf { it.value }

        assertThat(sessions)
            .withFailMessage(
                "$sessions sessions already ran in this JVM, so the measurement below would be vacuous. " +
                    "Restore the class ordering from junit-platform.properties and @Order(1) on this class."
            )
            .isZero()
    }

    private suspend fun runSession(voiceCallId: String) {
        mockGigaVoiceService.reset()
        withSession(testStub(), mockGigaVoiceService, gigaVoiceAgentMock) {
            session.sendRequest(contextRequest())
            session.sendRequest(settingsRequest(voiceCallId))
            mock.awaitRequest { it.hasSettings() }

            mock.sendResponse(outputTranscriptionResponse())
            session.awaitResponse()
        }
    }

    private suspend fun classesLoadedBy(block: suspend () -> Unit): Long {
        val classLoading = ManagementFactory.getClassLoadingMXBean()
        val before = classLoading.totalLoadedClassCount
        block()
        return classLoading.totalLoadedClassCount - before
    }

    private companion object {
        const val WARM_SESSIONS = 3
        const val MAX_EXTRA_CLASSES = 2200L

        val logger = KotlinLogging.logger { }
    }
}
