package ru.sbrf.dab2c.executor.library.sup.impl

import ru.sbrf.dab2c.executor.library.sup.api.ExecutorConfigService
import ru.sbrf.ufs.platform.config.v2.ExtendedConfigService
import ru.sbrf.ufs.platform.config.v2.ParameterValue
import ru.sbrf.ufs.platform.config.v2.RequestTemplate

class ExecutorConfigServiceImpl(
    private val requestTemplate: RequestTemplate,
    private val extendedConfigService: ExtendedConfigService
): ExecutorConfigService {

    override fun getParameterValue(paramName: String): ParameterValue = requestTemplate
        .buildRequest(paramName)
        .let { extendedConfigService.getParameters(it).getOne(it) }

}

inline fun <reified T> ExecutorConfigService.get(paramName: String): T {
    val parameterValue = getParameterValue(paramName)

    return when(T::class) {
        String::class -> parameterValue.string.get() as T
        Long::class -> parameterValue.long.get() as T
        Boolean::class -> parameterValue.boolean.get() as T
        else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
    }
}

