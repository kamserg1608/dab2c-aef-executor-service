package ru.sbrf.ufs.dab2c.core.executor.shared.commons.node

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.sbrf.ufs.platform.core.env.NodeTypeProvider

/**
 * Enables default [NodeTypeProvider].
 */
@Configuration(proxyBeanMethods = false)
class NodeTypeProviderAutoConfiguration {

    @Bean
    internal fun nodeTypeProvider(): NodeTypeProvider = DefaultNodeTypeProvider
}
