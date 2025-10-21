package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest.Companion.INVOKE_PATH
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters
import ru.sbrf.ufs.dab2c.core.executor.shared.monitoring.annotation.RestMonitored

/**
 * Invoke controller.
 */
@RestController
@RequestMapping(
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
    path = [INVOKE_PATH]
)
@SuppressFBWarnings("SPRING_ENDPOINT", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION")
class InvokeRest {

    /**
     * Invoke endpoint.
     */
    @PostMapping
    @RestMonitored(INVOKE)
    @PropagateLogParameters(INVOKE_PATH)
    fun invoke(): ResponseEntity<Unit> = ResponseEntity.ok(Unit)

    internal companion object {
        internal const val INVOKE = "INVOKE"
        internal const val INVOKE_PATH = "/invoke"
    }
}
