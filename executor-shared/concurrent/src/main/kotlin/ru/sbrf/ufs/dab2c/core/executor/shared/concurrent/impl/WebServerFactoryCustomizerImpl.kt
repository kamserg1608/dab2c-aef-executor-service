package ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.impl

import org.apache.coyote.ProtocolHandler
import org.apache.coyote.http11.Http11NioProtocol
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.config.properties.WebServerTypeProperties
import ru.sbrf.ufs.dab2c.core.executor.shared.concurrent.model.ExecutorType
import java.util.concurrent.Executors

/**
 * [TomcatServletWebServerFactory] used to apply [WebServerTypeProperties].
 */
class WebServerFactoryCustomizerImpl(
    private val webServerTypeProperties: WebServerTypeProperties
) : WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

    override fun customize(webServerFactory: TomcatServletWebServerFactory) {

        when (webServerTypeProperties.type) {

            ExecutorType.FIXED_THREAD -> webServerFactory.addProtocolHandlerCustomizers(
                TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
                    protocolHandler.executor = Executors.newFixedThreadPool(
                        webServerTypeProperties.fixedThreadPoolSize,
                        Thread.ofPlatform().name("protocol-pt-handler-", 1).factory()
                    )
                }
            )

            ExecutorType.VIRTUAL_THREAD_PER_TASK -> webServerFactory.addProtocolHandlerCustomizers(
                TomcatProtocolHandlerCustomizer<ProtocolHandler> { protocolHandler ->
                    protocolHandler.executor = Executors.newThreadPerTaskExecutor(
                        Thread.ofVirtual()
                            .name("protocol-vt-handler-", 1)
                            .factory()
                    )
                }
            )

            ExecutorType.USE_VIRTUAL_THREADS ->
                webServerFactory.addConnectorCustomizers(TomcatConnectorCustomizer {
                    (it.protocolHandler as Http11NioProtocol).setProperty("useVirtualThreads", "true")
                })

            ExecutorType.DEFAULT -> {}
        }
    }
}
