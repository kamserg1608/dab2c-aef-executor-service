package ru.sbrf.ufs.dab2c.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Входная точка для запуска Spring-Boot-приложения.
 */
@EnableAspectJAutoProxy
@SpringBootApplication(exclude = {JmsAutoConfiguration.class, ValidationAutoConfiguration.class})
public class ApplicationEntryPoint {

    /**
     * Запуск Spring-приложения
     * @param args Аргументы
     */
    public static void main(String[] args) {
        SpringApplication.run(ApplicationEntryPoint.class, args);
    }
}
