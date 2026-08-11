package ru.sbrf.dab2c.executor.library.testing.tracing

import com.google.protobuf.ByteString
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.common.v1.AnyValue
import io.opentelemetry.proto.common.v1.KeyValue
import io.opentelemetry.proto.trace.v1.ResourceSpans
import io.opentelemetry.proto.trace.v1.ScopeSpans
import io.opentelemetry.proto.trace.v1.Span
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TracingTestSupportTest {

    @Test
    fun `parses an export request into ParsedSpans grouped by parent`() {
        val parentId = "0102030405060708"
        val childId = "1112131415161718"
        val payload = ExportTraceServiceRequest.newBuilder()
            .addResourceSpans(
                ResourceSpans.newBuilder()
                    .addScopeSpans(
                        ScopeSpans.newBuilder()
                            .addSpans(
                                span(
                                    name = "parent",
                                    spanIdHex = parentId,
                                    attrs = mapOf("aef.kind" to "voice_session")
                                )
                            )
                            .addSpans(
                                span(
                                    name = "child",
                                    spanIdHex = childId,
                                    parentSpanIdHex = parentId,
                                    attrs = mapOf("aef.kind" to "voice_turn", "aef.session_id" to "S")
                                )
                            )
                    )
            )
            .build()
            .toByteArray()

        val spans = TracingTestSupport.parse(payload)
        assertEquals(2, spans.size)

        assertSpans(spans) {
            val turn = ofKind("voice_turn")
            assertNestedUnder(turn, "voice_session")
            assertAttributeNonEmpty(turn, "aef.session_id")
        }
    }

    private fun span(
        name: String,
        spanIdHex: String,
        parentSpanIdHex: String? = null,
        traceIdHex: String = "0102030405060708090a0b0c0d0e0f10",
        attrs: Map<String, String> = emptyMap()
    ): Span = Span.newBuilder()
        .setName(name)
        .setTraceId(ByteString.copyFrom(traceIdHex.hexToBytes()))
        .setSpanId(ByteString.copyFrom(spanIdHex.hexToBytes()))
        .also { b -> parentSpanIdHex?.let { b.setParentSpanId(ByteString.copyFrom(it.hexToBytes())) } }
        .also { b ->
            attrs.forEach { (k, v) ->
                b.addAttributes(
                    KeyValue.newBuilder().setKey(k).setValue(AnyValue.newBuilder().setStringValue(v)).build()
                )
            }
        }
        .build()

    private fun String.hexToBytes(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
