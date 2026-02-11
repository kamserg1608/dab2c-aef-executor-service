package ru.sbrf.dab2c.executor.library.audit.context

/**
 * Provides contextual audit information extracted from request/session.
 */
interface AuditContextProvider {

    /** Returns cookie value required to call audit endpoint, if available. */
    suspend fun cookieOrNull(): String?

    /** Returns EPK identifier, if available. */
    suspend fun epkIdOrNull(): String?
}
