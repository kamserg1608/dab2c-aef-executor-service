package ru.sbrf.dab2c.executor.voice.config.properties

import kotlinx.coroutines.SupervisorJob
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.clients.kap.producer.api.KapProducerClient
import ru.sbrf.dab2c.executor.library.common.CloseableCoroutineScope
import ru.sbrf.dab2c.executor.voice.service.api.AnalyticsPublisher
import ru.sbrf.dab2c.executor.voice.service.impl.KapAnalyticsPublisher

/**
 * Spring configuration for voice executor service.
 */
@Configuration
@EnableConfigurationProperties(
    value = [
        VoiceExecutorConfigurationProperties::class
    ]
)
class VoiceExecutorConfiguration {

    /** Outlives any single session, so post-processing survives the end of the call it belongs to. */
    @Bean(POST_PROCESS_SCOPE_BEAN_NAME, destroyMethod = "cancel")
    fun postProcessScope(): CloseableCoroutineScope = CloseableCoroutineScope(SupervisorJob())

    /** Stateless — safe to share across sessions. */
    @Bean
    fun analyticsPublisher(kapProducerClient: KapProducerClient): AnalyticsPublisher =
        KapAnalyticsPublisher(kapProducerClient)

    /** Name of the scope bean declared here. */
    companion object {
        const val POST_PROCESS_SCOPE_BEAN_NAME = "postProcessScope"
    }
}
