package ru.sbrf.ufs.dab2c.core.executor.shared.commons.rest.handling.model

import org.springframework.http.HttpStatus

/**
 * An enumeration of the error codes used in the application.
 *  Each enumeration value contains an error code and a message associated with this error.
 */
enum class ErrorCode(
    /**
     * Http status code.
     */
    val status: HttpStatus,
    /**
     * Code value.
     */
    val code: String,
    /**
     * Title for error.
     */
    val title: String,
) {
    /**
     * Error code for internal server errors related to Cbul Phoenix responses.
     */
    CBUL_ERROR(
        HttpStatus.OK,
        "00",
        "Cbul Phoenix ответил ошибкой"
    ),
    /**
     * Error code for not having access to save or receive master data.
     */
    NOT_ALLOWED_SUBSYSTEM_ERROR(
        HttpStatus.OK,
        "01",
        "Вызов запрещен"
    ),
    /**
     * Error code for validation errors.
     */
    VALIDATION_ERROR(
        HttpStatus.OK,
        "02",
        "Валидация запроса прошла не успешно"
    ),
    /**
     * Error code for functionality not available.
     */
    FUNCTIONALITY_NOT_AVAILABLE(
        HttpStatus.OK,
        "03",
        "Функционал не доступен"
    ),
    /**
     * Error code when ufsId not found.
     */
    UFS_ID_NOT_FOUND(
        HttpStatus.OK,
        "04",
        "Идентификатор клиента не найден"
    ),
    /**
     * Error code for calls not permitted by circuit breaker.
     */
    CALL_NOT_PERMITTED_BY_CIRCUIT_BREAKER(
        HttpStatus.OK,
        "98",
        "Вызов не разрешен"
    ),
    /**
     * Error code for another type of internal server error.
     */
    INTERNAL_SERVER_ERROR(
        HttpStatus.OK,
        "99",
        "Другая ошибка"
    ),
}
