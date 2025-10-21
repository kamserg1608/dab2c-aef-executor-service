package ru.sbrf.ufs.dab2c.core.executor.shared.commons.util.sanity

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.springframework.beans.BeansException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.api.SanityChecker
import ru.sbrf.ufs.dab2c.core.executor.shared.commons.sanity.config.StartupSanityCheckConfig

class StartupSanityCheckerTest {

    @Test
    fun contextStartUpFailed() {
        assertThatCode {
            val context = AnnotationConfigApplicationContext()
            context.register(ThrowingTestConfig::class.java)
            context.refresh()
        }.isInstanceOf(BeansException::class.java)
    }

    @Test
    fun contextStartUpSucceeded() {
        assertThatCode {
            val context = AnnotationConfigApplicationContext()
            context.register(OkTestConfig::class.java)
            context.refresh()
            context.close()
        }.doesNotThrowAnyException()
    }

    @Configuration
    @Import(StartupSanityCheckConfig::class)
    internal class ThrowingTestConfig {

        @Bean
        fun dummyCheck(): SanityChecker {
            val checker = mockk<SanityChecker>()
            every { checker.check() } throws RuntimeException("Ooops...")
            return checker
        }
    }

    @Configuration
    @Import(StartupSanityCheckConfig::class)
    internal class OkTestConfig {

        @Bean
        fun dummyCheck(): SanityChecker =
            mockk<SanityChecker>(relaxed = true)
    }
}
