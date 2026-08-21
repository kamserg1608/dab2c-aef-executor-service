@file:Suppress("TooManyFunctions")

package ru.sbrf.dab2c.executor.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import org.slf4j.MDC

private const val INTEGRATION_TYPE = "INTEGRATION"
private const val ERROR_CODE_SUCCESS = "0"
private const val ERROR_CODE_HTTP = "HTTP_ERROR"
private const val ERROR_CODE_GRPC = "GRPC_ERROR"

/**
 * Helper for logging integration calls (HTTP, gRPC).
 * Each call produces ONE log record AFTER completion with request/response details.
 */
object IntegrationLogger {

    private val logger = KotlinLogging.logger {}

    /**
     * Wraps a suspend HTTP call and logs a single integration record after completion.
     */
    @Suppress("LongParameterList", "LongMethod")
    suspend fun <T> logHttpCallSuspend(
        destinationSystem: String,
        destinationService: String,
        rqMessage: String,
        className: String,
        responseExtractor: (T) -> Pair<String, Int>,
        block: suspend () -> T
    ): T {
        val savedMdc = MDC.getCopyOfContextMap() ?: emptyMap()
        val startTime = System.currentTimeMillis()

        return try {
            val result = block()
            val executionTime = System.currentTimeMillis() - startTime
            val (rsMessage, statusCode) = responseExtractor(result)

            // Restore saved MDC first (may be on different thread after block())
            // then add integration fields on top
            MDC.setContextMap(savedMdc)
            setIntegrationSuccessMdc(
                destinationSystem, destinationService,
                rqMessage, rsMessage, executionTime, statusCode, className
            )
            logger.info { "HTTP $destinationService completed" }

            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            val statusCode = (e as? io.ktor.client.plugins.ResponseException)?.response?.status?.value
            // Restore saved MDC first, then add integration fields
            MDC.setContextMap(savedMdc)
            setIntegrationErrorMdc(
                destinationSystem, destinationService,
                rqMessage, executionTime, className, statusCode
            )
            logger.error(e) { "HTTP $destinationService failed: ${e.message}" }
            throw e
        } finally {
            // Restore back to SYSTEM type after logging
            MDC.setContextMap(savedMdc)
        }
    }

    /**
     * Logs a SINGLE gRPC event record (session start/end, errors).
     */
    fun logGrpcEvent(
        message: String,
        rqMessage: String? = null,
        rsMessage: String? = null,
        executionTime: Long? = null,
        error: Throwable? = null
    ) {
        val savedMdc = MDC.getCopyOfContextMap() ?: emptyMap()
        try {
            MDC.put(MdcKeys.TYPE, INTEGRATION_TYPE)
            MDC.put(MdcKeys.ERROR_CODE, if (error != null) ERROR_CODE_GRPC else ERROR_CODE_SUCCESS)
            rqMessage?.let { MDC.put(MdcKeys.RQ_MESSAGE, it) }
            rsMessage?.let { MDC.put(MdcKeys.RS_MESSAGE, it) }
            executionTime?.let { MDC.put(MdcKeys.EXECUTION_TIME_LONG, it.toString()) }

            if (error != null) {
                logger.error(error) { message }
            } else {
                logger.info { message }
            }
        } finally {
            MDC.setContextMap(savedMdc)
        }
    }

    @Suppress("LongParameterList")
    private fun setIntegrationSuccessMdc(
        destinationSystem: String,
        destinationService: String,
        rqMessage: String,
        rsMessage: String,
        executionTime: Long,
        statusCode: Int,
        className: String
    ) {
        MDC.put(MdcKeys.TYPE, INTEGRATION_TYPE)
        MDC.put(MdcKeys.DESTINATION_SYSTEM, destinationSystem)
        MDC.put(MdcKeys.DESTINATION_SERVICE, destinationService)
        MDC.put(MdcKeys.RQ_MESSAGE, rqMessage)
        MDC.put(MdcKeys.RS_MESSAGE, rsMessage)
        MDC.put(MdcKeys.EXECUTION_TIME_LONG, executionTime.toString())
        MDC.put(MdcKeys.STATUS_CODE, statusCode.toString())
        MDC.put(MdcKeys.CLASS_NAME, className)
        MDC.put(MdcKeys.ERROR_CODE, ERROR_CODE_SUCCESS)
    }

    @Suppress("LongParameterList")
    private fun setIntegrationErrorMdc(
        destinationSystem: String,
        destinationService: String,
        rqMessage: String,
        executionTime: Long,
        className: String,
        statusCode: Int? = null
    ) {
        MDC.put(MdcKeys.TYPE, INTEGRATION_TYPE)
        MDC.put(MdcKeys.DESTINATION_SYSTEM, destinationSystem)
        MDC.put(MdcKeys.DESTINATION_SERVICE, destinationService)
        MDC.put(MdcKeys.RQ_MESSAGE, rqMessage)
        MDC.put(MdcKeys.EXECUTION_TIME_LONG, executionTime.toString())
        MDC.put(MdcKeys.CLASS_NAME, className)
        MDC.put(MdcKeys.ERROR_CODE, ERROR_CODE_HTTP)
        statusCode?.let { MDC.put(MdcKeys.STATUS_CODE, it.toString()) }
    }
}
