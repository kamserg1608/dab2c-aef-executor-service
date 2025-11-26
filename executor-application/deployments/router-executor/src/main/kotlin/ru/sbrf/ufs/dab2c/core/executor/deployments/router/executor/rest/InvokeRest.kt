package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.sbrf.ufs.dab2c.acl.model.InvokeRequestSchema
import ru.sbrf.ufs.dab2c.acl.model.InvokeResponseSchema
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.rest.InvokeRest.Companion.INVOKE_PATH
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.api.InvokeProxyService
import ru.sbrf.ufs.dab2c.core.executor.shared.logging.annotation.PropagateLogParameters

/**
 * Invoke controller.
 */
// @SuppressFBWarnings("SPRING_ENDPOINT", "NAB_NEEDLESS_BOOLEAN_CONSTANT_CONVERSION")
@RestController
@RequestMapping(
    consumes = [MediaType.APPLICATION_JSON_VALUE],
    produces = [MediaType.APPLICATION_JSON_VALUE],
    path = [INVOKE_PATH]
)
class InvokeRest(
    private val invokeProxyService: InvokeProxyService
) {

    /**
     * Invoke endpoint.
     */
    @PostMapping
    @PropagateLogParameters(INVOKE_PATH)
    suspend fun invoke(
        @RequestBody invokeRequestSchema: InvokeRequestSchema
    ): ResponseEntity<InvokeResponseSchema> = ResponseEntity.ok(invokeProxyService.invoke(invokeRequestSchema))

    internal companion object {
        internal const val INVOKE_PATH = "/invoke"
    }
}
