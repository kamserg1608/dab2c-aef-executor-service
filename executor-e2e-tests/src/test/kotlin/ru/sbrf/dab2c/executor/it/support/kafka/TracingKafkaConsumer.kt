package ru.sbrf.dab2c.executor.it.support.kafka

import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import ru.sbrf.dab2c.executor.library.testing.tracing.ParsedSpan
import ru.sbrf.dab2c.executor.library.testing.tracing.TracingTestSupport
import java.time.Duration

/** Kafka consumer for OTLP-protobuf span records emitted by AEF SDK. */
object TracingKafkaConsumer {

    private const val TRACING_TOPIC = "aef-tracing-test"
    private const val POLL_INTERVAL_MS = 500L

    fun EmbeddedKafkaBroker.createTracingConsumer(): Consumer<String, ByteArray> {
        val props = KafkaTestUtils.consumerProps(
            "tracing-test-${System.currentTimeMillis()}",
            "true",
            this
        )
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"

        val consumer = DefaultKafkaConsumerFactory<String, ByteArray>(props).createConsumer()
        consumeFromAnEmbeddedTopic(consumer, TRACING_TOPIC)
        return consumer
    }

    /**
     * Collect spans flushed during [action]. When [voiceCallId] is provided, returns only spans
     * from the trace whose `voice_session.aef.settings` carries that call id — isolates the trace
     * from a concurrent neighbour test sharing the Spring context.
     */
    suspend fun EmbeddedKafkaBroker.collectTraceSpans(
        delayMs: Long = 500,
        timeout: Duration = Duration.ofSeconds(15),
        voiceCallId: String? = null,
        action: suspend () -> Unit
    ): List<ParsedSpan> {
        val target = voiceCallId?.let { id -> { spans: List<ParsedSpan> -> spans.any { it.hasCallId(id) } } }
        return resolveTrace(accumulate(delayMs, timeout, target, action), voiceCallId)
    }

    /**
     * Collect every span in the topic without grouping them into a single trace, waiting until one
     * matching [awaitSpan] shows up. Needed when the span of interest may belong to a trace of its
     * own; the result carries spans of neighbouring tests, so callers must filter.
     */
    suspend fun EmbeddedKafkaBroker.collectAllSpans(
        awaitSpan: (ParsedSpan) -> Boolean,
        delayMs: Long = 500,
        timeout: Duration = Duration.ofSeconds(15),
        action: suspend () -> Unit
    ): List<ParsedSpan> =
        accumulate(delayMs, timeout, { spans -> spans.any(awaitSpan) }, action)

    private suspend fun EmbeddedKafkaBroker.accumulate(
        delayMs: Long,
        timeout: Duration,
        target: ((List<ParsedSpan>) -> Boolean)?,
        action: suspend () -> Unit
    ): List<ParsedSpan> {
        val consumer = createTracingConsumer()
        action()
        delay(delayMs)

        val accumulated = mutableListOf<ParsedSpan>()
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            accumulated += poll(consumer)
            val targetFound = target != null && target(accumulated)
            if (target == null || targetFound) {
                if (targetFound) {
                    delay(delayMs)
                    accumulated += poll(consumer)
                }
                break
            }
            delay(POLL_INTERVAL_MS)
        }
        consumer.close()
        return accumulated
    }

    private fun poll(consumer: Consumer<String, ByteArray>): List<ParsedSpan> {
        val batch: ConsumerRecords<String, ByteArray> =
            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2))
        return TracingTestSupport.parseAll(batch.records(TRACING_TOPIC).map { it.value() })
    }

    private fun resolveTrace(accumulated: List<ParsedSpan>, voiceCallId: String?): List<ParsedSpan> {
        if (voiceCallId != null) {
            val target = accumulated.firstOrNull { it.hasCallId(voiceCallId) }?.traceId
            if (target != null) return accumulated.filter { it.traceId == target }
        }
        return accumulated.groupBy { it.traceId }.maxByOrNull { it.value.size }?.value.orEmpty()
    }

    private fun ParsedSpan.hasCallId(voiceCallId: String): Boolean =
        attributes["aef.settings"]?.contains("\"voiceCallId\":\"$voiceCallId\"") == true
}
