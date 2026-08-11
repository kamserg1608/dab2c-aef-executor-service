package ru.sbrf.dab2c.executor.clients.kap.producer.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.clients.kap.producer.impl.NoopKapProducerClient

/**
 * Configuration that provides a no-op KapProducerClient when KAP publishing is disabled.
 */
@Configuration
@ConditionalOnProperty(name = ["kafka.kap.producer.enabled"], havingValue = "false")
class KapProducerDisabledConfiguration {

    @Bean
    internal fun kapProducerClient(): KapProducerClient = NoopKapProducerClient
}
