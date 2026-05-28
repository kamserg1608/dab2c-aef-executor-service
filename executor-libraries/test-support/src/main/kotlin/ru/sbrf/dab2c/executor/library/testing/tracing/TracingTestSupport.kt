package ru.sbrf.dab2c.executor.library.testing.tracing

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import org.junit.jupiter.api.Assertions.assertNotNull
import io.opentelemetry.proto.trace.v1.Span as ProtoSpan

/**
 * Parses OTLP protobuf messages exported by AEF SDK's KafkaSpanExporter and
 * provides a small fluent assertion DSL for E2E tracing tests.
 *
 * The exporter emits `ExportTraceServiceRequest` payloads as Kafka record values.
 */
object TracingTestSupport {

    /** Decode a Kafka record value into a flat list of spans. */
    fun parse(payload: ByteArray): List<ParsedSpan> {
        val request = ExportTraceServiceRequest.parseFrom(payload)
        return request.resourceSpansList.flatMap { resource ->
            resource.scopeSpansList.flatMap { scope ->
                scope.spansList.map(::toParsed)
            }
        }
    }

    /** Aggregate multiple Kafka records (single trace can be split). */
    fun parseAll(payloads: Iterable<ByteArray>): List<ParsedSpan> =
        payloads.flatMap(::parse)

    private fun toParsed(proto: ProtoSpan): ParsedSpan = ParsedSpan(
        name = proto.name,
        traceId = proto.traceId.toByteArray().toHexString(),
        spanId = proto.spanId.toByteArray().toHexString(),
        parentSpanId = proto.parentSpanId?.toByteArray()
            ?.takeIf { it.any { byte -> byte.toInt() != 0 } }?.toHexString(),
        attributes = proto.attributesList.associate { it.key to attributeValue(it.value) }
    )

    private fun attributeValue(value: io.opentelemetry.proto.common.v1.AnyValue): String =
        when {
            value.hasStringValue() -> value.stringValue
            value.hasBoolValue() -> value.boolValue.toString()
            value.hasIntValue() -> value.intValue.toString()
            value.hasDoubleValue() -> value.doubleValue.toString()
            else -> value.toString()
        }
}

/** A single span flattened to the fields we care about in assertions. */
data class ParsedSpan(
    val name: String,
    val traceId: String,
    val spanId: String,
    val parentSpanId: String?,
    val attributes: Map<String, String>,
) {
    /** Convenience accessor for the AEF span-type attribute. */
    fun kind(): String? = attributes["aef.kind"]

    /** Convenience accessor for the AEF session identifier attribute. */
    fun sessionId(): String? = attributes["aef.session_id"]
}

/** Builds an in-memory index over a span list and exposes a small fluent DSL. */
class SpanAssertions(private val spans: List<ParsedSpan>) {

    /** Locate the first span with the given `aef.kind`; fails if none found. */
    fun ofKind(kind: String): ParsedSpan =
        spans.firstOrNull { it.kind() == kind }
            ?: error("No span with aef.kind=$kind. Captured: ${spans.map { "${it.name}/${it.kind()}" }}")

    /** All spans matching the kind, in capture order. */
    fun allOfKind(kind: String): List<ParsedSpan> =
        spans.filter { it.kind() == kind }

    /** Locate the direct parent of [child] by spanId; null when child is root. */
    fun parentOf(child: ParsedSpan): ParsedSpan? =
        child.parentSpanId?.let { pid -> spans.firstOrNull { it.spanId == pid } }

    /** Assert that `child` is nested inside a span of `parentKind`. */
    fun assertNestedUnder(child: ParsedSpan, parentKind: String) {
        val parent = parentOf(child)
        assertNotNull(parent) { "Span ${child.name}/${child.kind()} has no parent — expected $parentKind" }
        check(parent!!.kind() == parentKind) {
            "Expected ${child.name} to be nested under aef.kind=$parentKind, " +
                "got ${parent.kind()} (${parent.name})"
        }
    }

    /** Assert that the span has a non-empty value at [key]. */
    fun assertAttributeNonEmpty(span: ParsedSpan, key: String) {
        val value = span.attributes[key]
        check(!value.isNullOrEmpty()) { "Span ${span.name}/${span.kind()} has empty $key" }
    }

    /** Assert that the span attribute [key] equals [expected]. */
    fun assertAttributeEquals(span: ParsedSpan, key: String, expected: String) {
        val actual = span.attributes[key]
        check(actual == expected) {
            "Span ${span.name}/${span.kind()} expected $key=$expected, got $actual"
        }
    }
}

/** Fluent entry point: `assertSpans(parsed) { ofKind("voice_turn") ... }`. */
fun assertSpans(spans: List<ParsedSpan>, block: SpanAssertions.() -> Unit) {
    SpanAssertions(spans).block()
}

private fun ByteArray.toHexString(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte) }
