package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.CircuitBreakerParameters.Companion.CB_PARAMETERS_PREFIX
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.CircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.GlobalRecordedExceptionsCircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.config.ExtendedConfigServiceFactory
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.callback.EventType
import ru.sbrf.ufs.platform.config.v2.callback.ParameterEvent
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

private const val SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON = "sup/cb-service-specific-config.json"

@Suppress("FunctionMaxLength")
class CircuitBreakerParametersTest {

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `test getParameters should return service specific configuration`() {
        baseGetParametersTest(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
    }

    private fun baseGetParametersTest(filePath: String) {
        val configService = ExtendedConfigServiceFactory.buildFileBased(filePath)
        val circuitBreakerParameters = CircuitBreakerParameters(configService)

        val result = circuitBreakerParameters.getParameters("service-name")

        val expected = PmsCircuitBreakerConfig(
            serviceName = "service-name",
            isEnabled = true,
            failureRateThreshold = 50.0F,
            recordedExceptions = emptySet(),
            ignoredExceptions = emptySet(),
            permittedNumberOfCallsInHalfOpenState = 10,
            slidingWindow = 100,
            waitInOpenStateDuration = Duration.parse("PT1M")
        )
        Assertions.assertEquals(result, expected)
    }

    @Test
    fun `test getParameters should apply customizers before return configuration`() {
        val configService = ExtendedConfigServiceFactory.buildFileBased(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
        val exceptionsSet = setOf(IllegalStateException::class.java)
        val customizers = listOf(
            GlobalRecordedExceptionsCircuitBreakerConfigCustomizer(exceptionsSet),
            GlobalIgnoredExceptionsCircuitBreakerConfigCustomizer(exceptionsSet)
        )
        val circuitBreakerParameters = CircuitBreakerParameters(configService, customizers)

        val result = circuitBreakerParameters.getParameters("service-name")

        val expected = PmsCircuitBreakerConfig(
            serviceName = "service-name",
            isEnabled = true,
            failureRateThreshold = 50.0F,
            recordedExceptions = exceptionsSet,
            ignoredExceptions = exceptionsSet,
            permittedNumberOfCallsInHalfOpenState = 10,
            slidingWindow = 100,
            waitInOpenStateDuration = Duration.parse("PT1M")
        )
        Assertions.assertEquals(result, expected)
    }

    @Test
    fun `test getParameters should apply only service related customizers`() {
        val configService = ExtendedConfigServiceFactory.buildFileBased(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
        val otherServiceCustomizer = mockk<CircuitBreakerConfigCustomizer>()
        val thisServiceCustomizer = mockk<CircuitBreakerConfigCustomizer>()
        val customizers = listOf(
            otherServiceCustomizer,
            thisServiceCustomizer
        )
        val circuitBreakerParameters = CircuitBreakerParameters(configService, customizers)
        every { otherServiceCustomizer.supportsService(any()) }.returns(false)
        every { thisServiceCustomizer.supportsService(any()) }.returns(true)
        every { otherServiceCustomizer.customize(any()) }.returns(Unit)
        every { thisServiceCustomizer.customize(any()) }.returns(Unit)

        val result = circuitBreakerParameters.getParameters("service-name")

        val expected = PmsCircuitBreakerConfig(
            serviceName = "service-name",
            isEnabled = true,
            failureRateThreshold = 50.0F,
            recordedExceptions = emptySet(),
            ignoredExceptions = emptySet(),
            permittedNumberOfCallsInHalfOpenState = 10,
            slidingWindow = 100,
            waitInOpenStateDuration = Duration.parse("PT1M")
        )
        Assertions.assertEquals(result, expected)

        verify(exactly = 0) { otherServiceCustomizer.customize(any()) }
        verify(exactly = 1) { thisServiceCustomizer.customize(any()) }
    }

    @Test
    fun `test subscribe should add callback to callback map`() {
        val configService = ExtendedConfigServiceFactory.buildFileBased(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
        val parametersChangeCallbacks: ConcurrentMap<String, () -> Unit> = ConcurrentHashMap()
        val callback = "service-name" to { }
        val circuitBreakerParameters = CircuitBreakerParameters(
            configService = configService,
            parametersChangeCallbacks = parametersChangeCallbacks
        )

        circuitBreakerParameters.subscribe(callback.first, callback.second)

        Assertions.assertEquals(parametersChangeCallbacks.size, 1)
        Assertions.assertEquals(parametersChangeCallbacks[callback.first], callback.second)
    }

    @Test
    fun `test action should not invoke callbacks on not CB parameter change`() {
        val configService = ExtendedConfigServiceFactory.buildFileBased(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
        val parametersChangeCallbacks = ConcurrentHashMap<String, (() -> Unit)>().apply {
            this["service-one"] = mockk(relaxed = true)
            this["service-two"] = mockk(relaxed = true)
        }
        val circuitBreakerParameters = CircuitBreakerParameters(
            configService = configService,
            parametersChangeCallbacks = parametersChangeCallbacks
        )
        val parameterEvent = ParameterEvent.parameterEvent("some-event", EventType.CHANGE)

        circuitBreakerParameters.action(parameterEvent)

        parametersChangeCallbacks.forEach { (_, callback) -> verify(exactly = 0) { callback.invoke() } }
    }

    @Test
    fun `test action should invoke callback on service whose parameter change`() {
        val configService = ExtendedConfigServiceFactory.buildFileBased(SUP_CB_SERVICE_SPECIFIC_CONFIG_JSON)
        val parametersChangeCallbacks = ConcurrentHashMap<String, (() -> Unit)>().apply {
            this["service-one"] = mockk(relaxed = true)
            this["service-two"] = mockk(relaxed = true)
        }
        val circuitBreakerParameters = CircuitBreakerParameters(
            configService = configService,
            parametersChangeCallbacks = parametersChangeCallbacks
        )
        val parameterName = "${CB_PARAMETERS_PREFIX}.service-one.any-prop"
        val parameterEvent = ParameterEvent.parameterEvent(parameterName, EventType.CHANGE)

        circuitBreakerParameters.action(parameterEvent)

        verify(exactly = 1) { parametersChangeCallbacks["service-one"]!!.invoke() }
        verify(exactly = 0) { parametersChangeCallbacks["service-two"]!!.invoke() }
    }

    @Test
    fun `test postConstruct should subscribe to config service parameters updates`() {
        val configService = mockk<ExtendedConfigService>(relaxed = true)

        val circuitBreakerParameters = CircuitBreakerParameters(configService)
        circuitBreakerParameters.postConstruct()

        verify { configService.subscribe(circuitBreakerParameters) }
    }
}
