package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.core.task.support.TaskExecutorAdapter
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceFactory
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.api.ExecutorServiceProvider
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties.ExecutorTypeProperties
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties.WebServerTypeProperties
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl.ExecutorServiceFactoryImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl.ExecutorServiceProviderImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl.StaticExecutorServiceProviderImpl
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl.WebServerFactoryCustomizerImpl
import ru.sbrf.ufs.platform.core.env.ThreadPoolWrapper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Declares concurrent module beans.
 */
@Configuration
@EnableConfigurationProperties(
    value = [
        WebServerTypeProperties::class,
        ExecutorTypeProperties::class
    ]
)
class ConcurrentConfiguration {

    @Bean(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    internal fun asyncTaskExecutor(): AsyncTaskExecutor =
        TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor())

    @Bean
    internal fun webServerFactoryCustomize(
        webServerTypeProperties: WebServerTypeProperties,
    ) = WebServerFactoryCustomizerImpl(
        webServerTypeProperties
    )

    @Bean
    internal fun executorServiceFactory(): ExecutorServiceFactory = ExecutorServiceFactoryImpl()

    @Bean(SIMPLE_EXECUTOR_SERVICE_PROVIDER)
    internal fun executorServiceProvider(
        executorTypeProperties: ExecutorTypeProperties,
        executorServiceFactory: ExecutorServiceFactory
    ): ExecutorServiceProvider = ExecutorServiceProviderImpl(
        executorTypeProperties,
        executorServiceFactory
    )

    @Bean(name = [SIMPLE_EXECUTOR_SERVICE], destroyMethod = "shutdown")
    internal fun executorService(
        @Qualifier(SIMPLE_EXECUTOR_SERVICE_PROVIDER)
        executorServiceProvider: ExecutorServiceProvider
    ): ExecutorService = executorServiceProvider.getExecutorService()

    @Bean(THREAD_WRAPPER_EXECUTOR_SERVICE)
    internal fun threadExecutorService(
        @Qualifier(SIMPLE_EXECUTOR_SERVICE)
        executorService: ExecutorService
    ): ThreadPoolWrapper = ThreadPoolWrapper(executorService)

    @Bean(THREAD_WRAPPER_EXECUTOR_SERVICE_PROVIDER)
    internal fun staticExecutorServiceProvider(
        @Qualifier(THREAD_WRAPPER_EXECUTOR_SERVICE) executorService: ExecutorService
    ): ExecutorServiceProvider = StaticExecutorServiceProviderImpl(executorService)

    internal companion object {
        internal const val SIMPLE_EXECUTOR_SERVICE_PROVIDER = "SIMPLE_EXECUTOR_SERVICE_PROVIDER"
        internal const val SIMPLE_EXECUTOR_SERVICE = "SIMPLE_EXECUTOR_SERVICE"
        internal const val THREAD_WRAPPER_EXECUTOR_SERVICE = "THREAD_WRAPPER_EXECUTOR_SERVICE"
        internal const val THREAD_WRAPPER_EXECUTOR_SERVICE_PROVIDER = "THREAD_WRAPPER_EXECUTOR_SERVICE_PROVIDER"
    }
}
