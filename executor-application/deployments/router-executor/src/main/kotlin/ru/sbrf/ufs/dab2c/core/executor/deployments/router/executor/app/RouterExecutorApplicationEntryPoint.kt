package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config.RootConfiguration

/**
 * Spring-Boot application entrypoint.
 */
@SpringBootApplication
@Import(RootConfiguration::class)
class RouterExecutorApplicationEntryPoint

/**
 * Launch Spring-application.
 * @param args
 */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    SpringApplication.run(RouterExecutorApplicationEntryPoint::class.java, *args)
}
