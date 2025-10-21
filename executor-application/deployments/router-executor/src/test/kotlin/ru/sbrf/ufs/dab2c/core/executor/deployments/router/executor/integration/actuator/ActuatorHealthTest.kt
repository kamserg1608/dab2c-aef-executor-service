package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.actuator

import org.apache.http.client.methods.HttpGet
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.BaseIntegrationTest
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.helpers.PropertiesHelper.baseUrl
import java.io.IOException

class ActuatorHealthTest : BaseIntegrationTest() {

    @Test
    @Throws(IOException::class)
    fun actuatorHealth() {
        val request = HttpGet("$baseUrl/actuator/health")
        try {
            val response = client.execute(request)
            Assertions.assertEquals(response.statusLine.statusCode, 200)
        } finally {
            request.releaseConnection()
        }
    }
}
