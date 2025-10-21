package ru.sbrf.ufs.dab2c.core.executor.shared.commons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.dto.ErrorResponse
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.RestExceptionHandlingAdvice

class RestExceptionHandlingAdviceTest {

    private val exceptionHandler = RestExceptionHandlingAdvice(emptyList())

    @Test
    fun `handle AnotherException`() {
        val exceptionMessage = "Internal server error"
        val exception = Exception(exceptionMessage)

        val response: ResponseEntity<ErrorResponse> = exceptionHandler.handleException(exception)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.success).isFalse
    }
}
