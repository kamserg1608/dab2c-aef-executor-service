package ru.sbrf.dab2c.executor.clients.kap.producer.configuration

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.configuration.properties.KapProducerConfigurationProperties
import ru.sbrf.dab2c.executor.clients.kap.producer.impl.AsyncKapProducerClientDecorator
import ru.sbrf.dab2c.executor.clients.kap.producer.impl.KapProducerClientImpl

/**
 * Spring configuration for KAP Kafka producer client.
 */
@Configuration
@EnableConfigurationProperties(KapProducerConfigurationProperties::class)
@ConditionalOnProperty(name = ["kafka.kap.producer.enabled"], havingValue = "true", matchIfMissing = true)
class KapProducerConfiguration {

    @Bean(KAP_PRODUCER_OBJECT_MAPPER_BEAN_NAME)
    internal fun kapProducerObjectMapper(): ObjectMapper = ObjectMapper().apply {
        registerModule(kotlinModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    @Bean(KAP_PRODUCER_FACTORY_BEAN_NAME)
    internal fun kapProducerFactory(
        properties: KapProducerConfigurationProperties,
        @Qualifier(KAP_PRODUCER_OBJECT_MAPPER_BEAN_NAME) objectMapper: ObjectMapper
    ): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to properties.bootstrapServers,
            ProducerConfig.CLIENT_ID_CONFIG to properties.clientId,
            ProducerConfig.ACKS_CONFIG to properties.acks,
            ProducerConfig.RETRIES_CONFIG to properties.retries,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        val factory = DefaultKafkaProducerFactory<String, Any>(configProps)
        factory.valueSerializer = JsonSerializer(objectMapper)
        return factory
    }

    @Bean(KAP_KAFKA_TEMPLATE_BEAN_NAME)
    internal fun kapKafkaTemplate(
        @Qualifier(KAP_PRODUCER_FACTORY_BEAN_NAME) producerFactory: ProducerFactory<String, Any>
    ): KafkaTemplate<String, Any> = KafkaTemplate(producerFactory)

    @Bean(KAP_PRODUCER_SCOPE_BEAN_NAME)
    internal fun kapProducerScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Bean
    internal fun kapProducerClient(
        properties: KapProducerConfigurationProperties,
        @Qualifier(KAP_KAFKA_TEMPLATE_BEAN_NAME) kafkaTemplate: KafkaTemplate<String, Any>,
        @Qualifier(KAP_PRODUCER_SCOPE_BEAN_NAME) scope: CoroutineScope
    ): KapProducerClient = AsyncKapProducerClientDecorator(
        delegate = KapProducerClientImpl(properties, kafkaTemplate),
        scope = scope
    )

    internal companion object {
        internal const val KAP_PRODUCER_OBJECT_MAPPER_BEAN_NAME = "kapProducerObjectMapper"
        internal const val KAP_PRODUCER_FACTORY_BEAN_NAME = "kapProducerFactory"
        internal const val KAP_KAFKA_TEMPLATE_BEAN_NAME = "kapKafkaTemplate"
        internal const val KAP_PRODUCER_SCOPE_BEAN_NAME = "kapProducerScope"
    }
}
