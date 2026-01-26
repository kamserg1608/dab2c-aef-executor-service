package ru.sbrf.dab2c.executor.voice.model

/** Known request headers for gRPC metadata access. */
enum class RequestHeader(val headerName: String) {
    SESSION("session"),
    TOKEN("token"),
    EDU_ID("edu_id"),
    PROXY("proxy"),
    CHANNEL("channel"),
    PLATFORM("platform"),
    X_REQUEST_ID("x-request-id"),
}
