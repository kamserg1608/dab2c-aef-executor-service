package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.web.reactive.config.EnableWebFlux
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config.RootConfiguration
import ru.sbrf.ufs.healthcheck.HealthCheckAutoConfiguration
import ru.sbrf.ufs.healthcheck.HealthCheckStubAutoConfiguration

/**
 * Spring-Boot application entrypoint.
 */
@EnableWebFlux
@SpringBootApplication(
    exclude = [
        HealthCheckStubAutoConfiguration::class
    ]
)
@Import(value = [RootConfiguration::class, WebFluxAutoConfiguration::class])
class RouterExecutorApplicationEntryPoint

/**
 * Launch Spring-application.
 * @param args
 */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    SpringApplication.run(RouterExecutorApplicationEntryPoint::class.java, *args)
}
