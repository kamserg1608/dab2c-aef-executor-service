package ru.sbrf.ufs.dab2c.core.executor.shared.healthcheck

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.platform.healthcheck.Health

class ApplicationHealthCheckTest {

    @Test
    fun check() {
        val checker = ApplicationHealthCheck()
        Assertions.assertEquals(checker.check(), Health.OK)
    }
}
