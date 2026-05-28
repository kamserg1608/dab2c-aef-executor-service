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
        val consumer = createTracingConsumer()
        action()
        delay(delayMs)

        val accumulated = mutableListOf<ParsedSpan>()
        val deadline = System.currentTimeMillis() + timeout.toMillis()
        while (System.currentTimeMillis() < deadline) {
            val batch: ConsumerRecords<String, ByteArray> =
                KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2))
            accumulated += TracingTestSupport.parseAll(batch.records(TRACING_TOPIC).map { it.value() })
            val targetFound = voiceCallId != null && accumulated.any { it.hasCallId(voiceCallId) }
            if (voiceCallId == null || targetFound) {
                if (targetFound) {
                    delay(delayMs)
                    val tail = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(2))
                    accumulated += TracingTestSupport.parseAll(tail.records(TRACING_TOPIC).map { it.value() })
                }
                break
            }
            delay(POLL_INTERVAL_MS)
        }
        consumer.close()
        return resolveTrace(accumulated, voiceCallId)
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
