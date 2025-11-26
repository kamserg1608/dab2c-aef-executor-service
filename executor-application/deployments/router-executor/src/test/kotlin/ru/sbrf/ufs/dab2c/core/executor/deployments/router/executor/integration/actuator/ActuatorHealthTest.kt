package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.actuator

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.awaitBody
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.BaseIntegrationTest
import java.io.IOException

class ActuatorHealthTest : BaseIntegrationTest() {

    @Test
    @Throws(IOException::class)
    fun actuatorHealth() {
        val response = runBlocking {
            client.get()
                .uri("/actuator/health")
                .retrieve()
                .awaitBody<String>()
        }
        Assertions.assertEquals(UP_RESPONSE, response)
    }

    companion object {
        const val UP_RESPONSE = "{\"status\":\"UP\",\"groups\":[\"liveness\",\"readiness\"]}"
    }
}
