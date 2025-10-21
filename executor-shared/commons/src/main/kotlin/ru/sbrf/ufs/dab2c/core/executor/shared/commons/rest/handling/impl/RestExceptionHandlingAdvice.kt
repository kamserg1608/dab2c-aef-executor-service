package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl

import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.dto.ErrorResponse
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.api.ExceptionMapper
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.impl.RestExceptionHandlingAdvice.Companion.ORDER
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model.ErrorCode

/**
 * The main class for exception handling.
 */
@ControllerAdvice
@Order(ORDER)
class RestExceptionHandlingAdvice(
    private val exceptionMappers: List<ExceptionMapper<out Throwable>>,
) {
    private val logger: Logger = LoggerFactory.getLogger(RestExceptionHandlingAdvice::class.java)

    /**
     * Handles exceptions that occur within the application.
     *
     * This method intercepts all exceptions that have not been handled by other
     * exception handlers. It returns the corresponding error code and message
     * in the `ErrorResponse` format.
     *
     */
    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ErrorResponse> {

        logger.error("An error occurred: ${ex.message}. Exception type: ${ex::class.simpleName}", ex)

        val errorCode = exceptionMappers.firstNotNullOfOrNull { it.map(ex) } ?: ErrorCode.INTERNAL_SERVER_ERROR

        val error = "${ errorCode.code } ${ errorCode.title } ${ ExceptionUtils.getRootCause(ex).message }"
        return ResponseEntity
            .status(errorCode.status)
            .body(ErrorResponse(success = false, errors = listOf(error)))
    }

    /**
     * Companion object for the RestExceptionHandlingAdvice class.
     */
    companion object {
        /**
         * Order of the advice.
         */
        const val ORDER = Ordered.LOWEST_PRECEDENCE - 1000
    }
}
