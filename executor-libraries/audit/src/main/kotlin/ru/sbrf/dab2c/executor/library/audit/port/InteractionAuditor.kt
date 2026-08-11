package ru.sbrf.dab2c.executor.library.audit.port

import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest

/** Port for auditing service interactions. */
interface InteractionAuditor {

    /** Records a successful interaction. */
    suspend fun success(request: InteractionAuditRequest)

    /** Records a failed interaction. */
    suspend fun failed(request: InteractionAuditRequest)
}
