package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.methods.HttpPost
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.BaseIntegrationTest
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.helpers.HttpClientHelper
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.helpers.PropertiesHelper.baseUrl
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest.Companion.INVOKE_PATH

class InvokeRestIntegrationTest : BaseIntegrationTest() {

    lateinit var helper: HttpClientHelper

    @Autowired
    lateinit var mapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        helper = HttpClientHelper(mapper, client)
    }

    @Test
    @Disabled
    fun updateRequestTest() {
        val request = HttpPost("$baseUrl$INVOKE_PATH")
        val response = helper.execute(request, ResponseEntity::class.java)
        Assertions.assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }
}
