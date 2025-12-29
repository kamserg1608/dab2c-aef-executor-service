package ru.sbrf.dab2c.executor.library.sup.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.dab2c.executor.library.sup.api.ExecutorConfigService
import ru.sbrf.dab2c.executor.library.sup.impl.ExecutorConfigServiceImpl
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.RequestTemplate
import ru.sbrf.ufs.platform.config.v2.RequestTemplateBuilder

@Configuration
class SupParanetersConfiguration {

    @Bean(EXECUTER_REQUEST_TEMPLATE_BEAN_NAME)
    internal fun executorRequestTemplate(): RequestTemplate = RequestTemplateBuilder.builder().attributeNames().build()

    @Bean
    internal fun executorConfigService(
        @Qualifier(EXECUTER_REQUEST_TEMPLATE_BEAN_NAME)
        requestTemplate: RequestTemplate,
        extendedConfigService: ExtendedConfigService
    ): ExecutorConfigService = ExecutorConfigServiceImpl(
        requestTemplate,
        extendedConfigService
    )

    internal companion object {
        internal const val EXECUTER_REQUEST_TEMPLATE_BEAN_NAME = "executorRequestTemplate"
    }

}