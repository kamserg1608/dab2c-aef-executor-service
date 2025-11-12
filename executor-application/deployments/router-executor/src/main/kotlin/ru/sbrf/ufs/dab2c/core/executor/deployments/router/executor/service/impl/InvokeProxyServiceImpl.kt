package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.impl

import ru.sbrf.ufs.dab2c.acl.model.ACLMessageContentOutput
import ru.sbrf.ufs.dab2c.acl.model.InvokeRequestSchema
import ru.sbrf.ufs.dab2c.acl.model.InvokeResponseSchema
import ru.sbrf.ufs.dab2c.acl.model.OutgoingMessageOutput
import ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.service.api.InvokeProxyService

class InvokeProxyServiceImpl : InvokeProxyService {

    override suspend fun invoke(invokeRequestSchema: InvokeRequestSchema): InvokeResponseSchema {
        return InvokeResponseSchema()
            .outgoing(
                OutgoingMessageOutput()
                    .content(
                        ACLMessageContentOutput()
                            .payload(
                                mapOf(
                                    "first" to "second"
                                )
                            )
                    )
            )
    }
}