package ru.sbrf.dab2c.executor.voice.model

/** Known request headers for gRPC metadata access. */
enum class RequestHeader(val headerName: String) {
    SESSION("x-session"),
    TOKEN("x-token"),
    EDU_ID("x-eduid"),
    PROXY("proxy"),
    CHANNEL("x-channel"),
    PLATFORM("x-platform"),
    X_REQUEST_ID("x-request-id"),
}
