package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.ConcurrentConfiguration
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.ConcurrentConfiguration.Companion.THREAD_WRAPPER_EXECUTOR_SERVICE_PROVIDER
import ru.sbrf.ufs.platform.core.env.PlatformEnvironmentInternal
import ru.sbrf.ufs.platform.logger.LoggerFactory
import ru.sbrf.ufs.platform.logger.Params
import java.util.concurrent.Callable

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [ConcurrentConfiguration::class])
class ThreadWrapperExecutorServiceTest {

    @MockBean
    private lateinit var platformEnvironment: PlatformEnvironmentInternal

    @Autowired
    @Qualifier(THREAD_WRAPPER_EXECUTOR_SERVICE_PROVIDER)
    private lateinit var executorServiceProvider: ExecutorServiceProvider

    @Test
    fun `test log context transfer between threads using the executor service`() {

        LoggerFactory.getLoggerContext().put(Params.BUSINESS_ID, "12345678")
        LoggerFactory.getLoggerContext().put(Params.RQ_UID, "123")
        val mainThreadLoggerContext = LoggerFactory.getLoggerContext().asMap()

        val future = executorServiceProvider.getExecutorService().submit(Callable {
            LoggerFactory.getLoggerContext().asMap()
        })

        assertEquals(mainThreadLoggerContext, future.get())
    }
}
