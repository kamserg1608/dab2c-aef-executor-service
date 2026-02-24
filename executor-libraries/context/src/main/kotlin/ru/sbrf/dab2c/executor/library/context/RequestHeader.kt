package ru.sbrf.dab2c.executor.library.context

/**
 * Standard request header names propagated through the coroutine context.
 */
enum class RequestHeader(val headerName: String) {
    SESSION("x-session"),
    TOKEN("x-token"),
    EDU_ID("x-eduid"),
    CHANNEL("x-channel"),
    PLATFORM("x-platform"),
    X_REQUEST_ID("x-request-id"),
}
