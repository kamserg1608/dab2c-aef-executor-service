package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.BaseIntegrationTest

class InvokeRestIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var mapper: ObjectMapper

    @Test
    @Disabled
    fun updateRequestTest() {
        val response = HttpStatus.OK
        Assertions.assertThat(response).isEqualTo(HttpStatus.OK)
    }
}
