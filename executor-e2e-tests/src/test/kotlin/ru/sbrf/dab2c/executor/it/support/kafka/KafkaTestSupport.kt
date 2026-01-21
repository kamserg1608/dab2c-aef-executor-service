package ru.sbrf.dab2c.executor.it.support.kafka

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import kotlinx.coroutines.delay
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.utils.KafkaTestUtils
import java.time.Duration

/**
 * Kafka test utilities for integration tests.
 */
object KafkaTestSupport {

    val objectMapper: ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun EmbeddedKafkaBroker.createTestConsumer(topic: String): Consumer<String, String> {
        val consumerProps = KafkaTestUtils.consumerProps(
            "test-group-${System.currentTimeMillis()}",
            "true",
            this
        )
        consumerProps[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java
        consumerProps[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "latest"

        val consumerFactory = DefaultKafkaConsumerFactory<String, String>(consumerProps)
        val consumer = consumerFactory.createConsumer()
        this.consumeFromAnEmbeddedTopic(consumer, topic)
        return consumer
    }

    suspend inline fun <reified T> consumeRecords(
        consumer: Consumer<String, String>,
        topic: String,
        delayMs: Long = 1000,
        timeout: Duration = Duration.ofSeconds(10),
        filter: (T) -> Boolean = { true }
    ): List<T> {
        delay(delayMs)

        val records: ConsumerRecords<String, String> = KafkaTestUtils.getRecords(consumer, timeout)
        consumer.close()

        return records.records(topic).toList()
            .map { objectMapper.readValue(it.value(), T::class.java) }
            .filter(filter)
    }

    suspend inline fun <reified T> EmbeddedKafkaBroker.awaitRecords(
        topic: String,
        delayMs: Long = 1000,
        timeout: Duration = Duration.ofSeconds(10),
        noinline filter: (T) -> Boolean = { true }
    ): List<T> {
        val consumer = createTestConsumer(topic)
        return consumeRecords(consumer, topic, delayMs, timeout, filter)
    }

    suspend inline fun <reified T> EmbeddedKafkaBroker.withConsumer(
        topic: String,
        delayMs: Long = 1000,
        timeout: Duration = Duration.ofSeconds(10),
        noinline filter: (T) -> Boolean = { true },
        action: () -> Unit
    ): List<T> {
        val consumer = createTestConsumer(topic)
        action()
        return consumeRecords(consumer, topic, delayMs, timeout, filter)
    }
}
