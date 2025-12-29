package ru.sbrf.dab2c.executor.library.sup.api

import ru.sbrf.ufs.platform.config.v2.ParameterValue

interface ExecutorConfigService {

    fun getParameterValue(paramName: String): ParameterValue

}