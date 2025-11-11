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
        val response = client.get()
            .uri("/actuator/health")
            .retrieve()
        Assertions.assertEquals(response.bodyToMono(String::class.java), "200")
    }
}
