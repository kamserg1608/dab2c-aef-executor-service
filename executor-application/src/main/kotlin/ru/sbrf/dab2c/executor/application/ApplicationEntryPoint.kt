package ru.sbrf.dab2c.executor.application

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Входная точка для запуска Spring-Boot-приложения.
 */
@SpringBootApplication
@ComponentScan(basePackages = ["ru.sbrf.dab2c.executor"])
class ApplicationEntryPoint

/**
 * Запуск Spring-приложения.
 * @param args Аргументы
 */
@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    SpringApplication.run(ApplicationEntryPoint::class.java, *args)
}
