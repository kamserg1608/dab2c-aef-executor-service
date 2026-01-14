package ru.sbrf.dab2c.executor.it.tests.http

import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest

/**
 * Integration tests for HTTP management endpoints (actuator).
 */
class ManagementEndpointsTest : BaseGigaVoiceIntegrationTest() {

    @Test
    fun actuatorHealth() = runTest {
        val response = httpClient.get("${servletContext.contextPath}/actuator/health")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
