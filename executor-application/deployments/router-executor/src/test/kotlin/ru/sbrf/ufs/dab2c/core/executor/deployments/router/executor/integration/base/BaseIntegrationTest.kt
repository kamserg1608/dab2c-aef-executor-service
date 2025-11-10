package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPoint
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config.SpyBeanConfiguration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.configuration.WrapConfiguration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.initializer.TestApplicationContextInitializer
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.configuration.RestObjectMapperConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
    classes = [
        RouterExecutorApplicationEntryPoint::class,
        WrapConfiguration::class,
        RestObjectMapperConfiguration::class,
        SpyBeanConfiguration::class
    ]
)
@ActiveProfiles(profiles = ["STUB", "stubMode"])
@ContextConfiguration(initializers = [TestApplicationContextInitializer::class])
@AutoConfigureMockMvc
abstract class BaseIntegrationTest {

    protected val client: WebClient = WebClient.create()

}
