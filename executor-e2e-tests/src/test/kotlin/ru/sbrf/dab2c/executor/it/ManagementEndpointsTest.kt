package ru.sbrf.dab2c.executor.it

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ManagementEndpointsTest : BaseGigaVoiceIntegrationTest() {

    data class EnvironmentResponse(
        val success: Boolean,
        val body: EnvironmentBody
    )

    data class EnvironmentBody(
        val subsystem: String
    )

    @Test
    fun environmentProduct() = runTest {
        val response = httpClient.get("${servletContext.contextPath}/environment/product")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = response.body<EnvironmentResponse>()

        assertTrue(responseBody.success)
        assertEquals("EXECUTOR", responseBody.body.subsystem)
    }

    @Test
    fun actuatorHealth() = runTest {
        val response = httpClient.get("${servletContext.contextPath}/healthcheck")
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
