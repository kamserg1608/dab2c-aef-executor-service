package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.util

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.util.ReportableThrowableUtils.getReportableThrowable
import java.lang.reflect.UndeclaredThrowableException

class ReportableThrowableUtilsTest {
    @Test
    fun noChainTest() {
        val originalThrowable: Throwable = RuntimeException()
        val resultThrowable = getReportableThrowable(originalThrowable)
        Assertions.assertThat(resultThrowable).isEqualTo(originalThrowable)
    }

    @Test
    fun declaredChainTest() {
        val originalThrowable: Throwable = IllegalStateException(RuntimeException())
        val resultThrowable = getReportableThrowable(originalThrowable)
        Assertions.assertThat(resultThrowable).isEqualTo(originalThrowable)
    }

    @Test
    fun undeclaredThrowableChainTest() {
        val expectedThrowable: Throwable = IllegalStateException(RuntimeException())
        val originalThrowable: Throwable = UndeclaredThrowableException(expectedThrowable)
        val resultThrowable = getReportableThrowable(originalThrowable)
        Assertions.assertThat(resultThrowable).isEqualTo(expectedThrowable)
    }
}
