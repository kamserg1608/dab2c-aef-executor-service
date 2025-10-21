package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base

import io.mockk.clearAllMocks
import org.apache.http.impl.client.HttpClientBuilder
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPoint
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.config.SpyBeanConfiguration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.configuration.WrapConfiguration
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.base.initializer.TestApplicationContextInitializer
import ru.sbrf.ufs.dab2c.core.executor.shared.cbul.api.ManagedCbulDaoService
import ru.sbrf.ufs.dab2c.core.executor.shared.rest.mapper.configuration.RestObjectMapperConfiguration
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.http.api.PooledHttpClient
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.http.impl.PooledHttpClientImpl

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

    protected val client: PooledHttpClient = PooledHttpClientImpl.create(HttpClientBuilder.create())

    @Autowired
    protected lateinit var cbulDaoService: ManagedCbulDaoService

    @AfterEach
    fun afterEach() {
        client.releaseConnections()
        cbulDaoService.resetStorage()
        clearAllMocks()
    }
}
