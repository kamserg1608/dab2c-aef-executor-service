package ru.sbrf.dab2c.executor.library.tracing.facade

import io.opentelemetry.api.common.AttributeKey

/** AEF span attribute keys for attributes the SDK does not set itself. */
object AefAttributeKeys {
    val INPUT: AttributeKey<String> = AttributeKey.stringKey("aef.input")
    val OUTPUT: AttributeKey<String> = AttributeKey.stringKey("aef.output")

    val WARNING: AttributeKey<String> = AttributeKey.stringKey("aef.warning")
    val ERROR: AttributeKey<String> = AttributeKey.stringKey("aef.error")
    val LLM_TOTAL_TOKENS: AttributeKey<Long> = AttributeKey.longKey("aef.llm.total_tokens")

    val REQUEST_METHOD: AttributeKey<String> = AttributeKey.stringKey("aef.request.method")
    val REQUEST_PATH: AttributeKey<String> = AttributeKey.stringKey("aef.request.path")
    val REQUEST_BODY: AttributeKey<String> = AttributeKey.stringKey("aef.request.body")
    val REQUEST_HEADERS: AttributeKey<String> = AttributeKey.stringKey("aef.request.headers")
    val RESPONSE_BODY: AttributeKey<String> = AttributeKey.stringKey("aef.response.body")
    val RESPONSE_HEADERS: AttributeKey<String> = AttributeKey.stringKey("aef.response.headers")
    val RESPONSE_STATUS_CODE: AttributeKey<String> = AttributeKey.stringKey("aef.response.status_code")
}
