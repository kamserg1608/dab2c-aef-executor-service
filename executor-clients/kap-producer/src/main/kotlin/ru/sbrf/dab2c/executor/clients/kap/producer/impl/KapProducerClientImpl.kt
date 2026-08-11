package ru.sbrf.dab2c.executor.clients.kap.producer.impl

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.future.await
import org.springframework.kafka.core.KafkaTemplate
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.configuration.properties.KapProducerConfigurationProperties
import ru.sbrf.dab2c.executor.clients.kap.producer.model.AgentAnalyticsEnvelope
import ru.sbrf.dab2c.executor.clients.kap.producer.model.DialogEnvelope

private val logger = KotlinLogging.logger {}

/**
 * Implementation of KapProducerClient using Spring Kafka.
 */
class KapProducerClientImpl(
    private val properties: KapProducerConfigurationProperties,
    private val kafkaTemplate: KafkaTemplate<String, Any>
) : KapProducerClient {

    override suspend fun publishDialog(dialog: DialogEnvelope) {
        val topic = properties.dialogsTopic
        logger.debug { "Publishing dialog to topic $topic, id=${dialog.id}" }
        val result = kafkaTemplate.send(topic, dialog.id, dialog).toCompletableFuture().await()
        logger.info { "Published dialog to $topic, id=${dialog.id}, offset=${result.recordMetadata.offset()}" }
    }

    override suspend fun publishAgentAnalytics(analytics: AgentAnalyticsEnvelope) {
        val topic = properties.agentsTopic
        logger.debug { "Publishing agent analytics to topic $topic, id=${analytics.id}" }
        val result = kafkaTemplate.send(topic, analytics.id, analytics).toCompletableFuture().await()
        val offset = result.recordMetadata.offset()
        logger.info { "Published agent analytics to $topic, id=${analytics.id}, offset=$offset" }
    }
}
