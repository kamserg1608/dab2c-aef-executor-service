package ru.sbrf.ufs.dab2c.core.executor.shared.logging.service.api

import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters

/**
 * Service for processing log parameters.
 */
interface LogParametersService {

    /**
     * Processes the [args] and adds them to the logging context based on [parameters].
     */
    fun processParameters(args: Array<Any>, parameters: PropagateLogParameters)

    /**
     * Restores context after [processParameters] call.
     */
    fun restoreContext()
}
