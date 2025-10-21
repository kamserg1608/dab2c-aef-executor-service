package ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect

import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.api.LogParametersService

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [LogParametersPropagatorAspectTestConfiguration::class])
class LogParametersPropagatorAspectTest {

    @Autowired
    private lateinit var testController: LogParametersPropagatorController

    @Autowired
    private lateinit var logParametersService: LogParametersService

    @BeforeEach
    fun setUp() {
        every { logParametersService.processParameters(any(), any()) } just runs
        every { logParametersService.restoreContext() } just runs
    }

    @Test
    fun `should proxy is applied`() {
        assertThat(AopUtils.isAopProxy(testController)).isTrue()
    }

    @Test
    fun `should process parameters and call restoreContext`() {

        testController.testMethod("user")

        verify(exactly = 1) {
            logParametersService.processParameters(
                arrayOf("user"),
                any<PropagateLogParameters>()
            )
        }
        verify(exactly = 1) { logParametersService.restoreContext() }
    }
}
