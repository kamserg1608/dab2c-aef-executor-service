package ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters

import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.customizer.CircuitBreakerConfigCustomizer
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsCircuitBreakerConfig
import ru.sbrf.ufs.dab2c.core.executor.shared.circuitbreaker.parameters.model.PmsParameter
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.ParameterValue
import ru.sbrf.ufs.platform.config.v2.RequestTemplateBuilder
import ru.sbrf.ufs.platform.config.v2.callback.ParameterEvent
import ru.sbrf.ufs.platform.config.v2.callback.ParameterEventCallback
import javax.annotation.PostConstruct
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Circuit breaker configuration parameters. */
@Suppress("TooGenericExceptionCaught")
class CircuitBreakerParameters(
    private val configService: ExtendedConfigService,
    private val customizers: List<CircuitBreakerConfigCustomizer> = emptyList(),
    private val parametersChangeCallbacks: ConcurrentMap<String, () -> Unit> = ConcurrentHashMap(),
) : ParameterEventCallback {

    /**
     * Subscribes to CircuitBreaker SUP-parameters.
     */
    @PostConstruct
    fun postConstruct() {
        configService.subscribe(this)
    }

    override fun action(event: ParameterEvent) {

        if (!event.name.startsWith(CB_PARAMETERS_PREFIX)) return

        val updatedServiceName = event.name.substring(CB_PARAMETERS_PREFIX.length + 1).split(".")[0]

        parametersChangeCallbacks
            .filter { (service, _) -> service == updatedServiceName }
            .forEach { (_, callback) -> callback.invoke() }
    }

    /** Subscribe to configuration parameters change. */
    fun subscribe(serviceName: String, callback: () -> Unit) {
        parametersChangeCallbacks[serviceName] = callback
    }

    /** Provides configuration parameters by service name. */
    fun getParameters(serviceName: String): PmsCircuitBreakerConfig {
        val pmsCircuitBreakerConfig = PmsCircuitBreakerConfig(serviceName = serviceName)

        for (parameter in CIRCUITBREAKER_PARAMETERS) getSingleParameter(serviceName, parameter, pmsCircuitBreakerConfig)

        customizers
            .filter { it.supportsService(serviceName) }
            .forEach { it.customize(pmsCircuitBreakerConfig) }

        return pmsCircuitBreakerConfig
    }

    private fun <Type> getSingleParameter(
        serviceName: String,
        parameter: PmsParameter<Type, PmsCircuitBreakerConfig>,
        pmsCircuitBreakerConfig: PmsCircuitBreakerConfig
    ) {
        try {
            val parameterValue = getParameter(serviceName, parameter.name)
                .let { parameter.extractor.invoke(it) }
                .get()
            parameter.enricher.invoke(pmsCircuitBreakerConfig, parameterValue)
        } catch (throwable: Throwable) {
            val unresolvedParamName = "$CB_PARAMETERS_PREFIX.$serviceName.${parameter.name}"
            throw IllegalStateException("Failed to resolve $unresolvedParamName for service $serviceName", throwable)
        }
    }

    private fun getParameter(serviceName: String, parameterName: String): ParameterValue {
        val fullParameterName = "$CB_PARAMETERS_PREFIX.$serviceName.$parameterName"
        val configRequest = RequestTemplateBuilder.builder().attributeNames().build().buildRequest(fullParameterName)
        return configService.getParameters(configRequest).getOne(configRequest)
    }

    internal companion object {
        internal const val CB_PARAMETERS_PREFIX: String = "dab2c.executor.circuit_breaker"

        internal val CIRCUITBREAKER_PARAMETERS: List<PmsParameter<out Any, PmsCircuitBreakerConfig>> = listOf(
            PmsParameter(
                name = "enabled",
                type = Boolean::class.java,
                extractor = ParameterValue::getBoolean
            ) { config, parameterValue -> config.isEnabled = parameterValue },
            PmsParameter(
                name = "failure_rate_threshold",
                type = Long::class.java,
                extractor = ParameterValue::getLong
            ) { config, parameterValue -> config.failureRateThreshold = parameterValue.toFloat() },
            PmsParameter(
                name = "permitted_number_of_calls_in_half_open_state",
                type = Long::class.java,
                extractor = ParameterValue::getLong
            ) { config, parameterValue -> config.permittedNumberOfCallsInHalfOpenState = parameterValue.toInt() },
            PmsParameter(
                name = "sliding_window",
                type = Long::class.java,
                extractor = ParameterValue::getLong
            ) { config, parameterValue -> config.slidingWindow = parameterValue.toInt() },
            PmsParameter(
                name = "wait_in_open_state_duration_seconds",
                type = String::class.java,
                extractor = ParameterValue::getString
            ) { config, parameterValue -> config.waitInOpenStateDuration = Duration.parse(parameterValue) },
        )
    }
}
