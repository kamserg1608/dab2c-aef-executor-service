package ru.sbrf.ufs.dab2c.core.executor.shared.logging.aspect

import org.springframework.http.ResponseEntity
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.LogParameter
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters

internal open class LogParametersPropagatorController {

    @PropagateLogParameters(SERVICE_NAME, true)
    open fun testMethod(@LogParameter user: String): ResponseEntity<String> =
        ResponseEntity.ok("Hello $user")

    companion object {
        const val SERVICE_NAME = "test"
    }
}
