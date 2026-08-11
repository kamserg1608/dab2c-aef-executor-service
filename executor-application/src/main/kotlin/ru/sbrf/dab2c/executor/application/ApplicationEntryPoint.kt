package ru.sbrf.dab2c.executor.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/** Spring Boot application entry point. */
@SpringBootApplication
@ComponentScan(basePackages = ["ru.sbrf.dab2c.executor"])
class ApplicationEntryPoint

/** Starts the Spring application. */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    SpringApplication.run(ApplicationEntryPoint::class.java, *args)
}
