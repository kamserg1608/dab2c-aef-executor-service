package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.api

import ru.sbrf.ufs.dab2c.acl.model.InvokeRequestSchema
import ru.sbrf.ufs.dab2c.acl.model.InvokeResponseSchema

/**
 * Proxy interface.
 */
interface InvokeProxyService {

    /**
     * Invoke method.
     */
    suspend fun invoke(
        invokeRequestSchema: InvokeRequestSchema
    ): InvokeResponseSchema
}
