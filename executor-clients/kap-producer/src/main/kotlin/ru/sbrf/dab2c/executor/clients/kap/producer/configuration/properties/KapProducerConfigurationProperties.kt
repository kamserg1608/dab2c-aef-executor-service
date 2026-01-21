package ru.sbrf.dab2c.executor.clients.kap.producer.configuration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for KAP Kafka producer.
 */
@ConfigurationProperties(prefix = "kafka.kap.producer")
data class KapProducerConfigurationProperties(
    val bootstrapServers: String,
    val dialogsTopic: String,
    val agentsTopic: String,
    val clientId: String = "AVG",
    val acks: String = "all",
    val retries: Int = 3,
    val enabled: Boolean = true
)
