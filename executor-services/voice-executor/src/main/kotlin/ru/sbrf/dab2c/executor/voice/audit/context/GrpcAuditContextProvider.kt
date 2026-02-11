package ru.sbrf.dab2c.executor.voice.audit.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.sbrf.dab2c.executor.library.audit.context.AuditContextProvider
import ru.sbrf.dab2c.executor.voice.grpc.context.GrpcMetadataContext

/**
 * Reads cookie/epkId from gRPC metadata.
 *
 * Keys are normalized to lowercase in RequestMetadata (as per your implementation).
 * We try several common keys to avoid tight coupling to a specific header enum.
 */
class GrpcAuditContextProvider : AuditContextProvider {

    override suspend fun cookieOrNull(): String? = withContext(Dispatchers.Unconfined) {
        val md = GrpcMetadataContext.fromGrpcThread()
        md[COOKIE] ?: md[X_COOKIE] ?: md[SET_COOKIE]
    }

    override suspend fun epkIdOrNull(): String? = withContext(Dispatchers.Unconfined) {
        val md = GrpcMetadataContext.fromGrpcThread()
        md[EPK_ID] ?: md[X_EPK_ID] ?: md[CLIENT_EPK_ID]
    }

    private companion object {
        private const val COOKIE = "cookie"
        private const val X_COOKIE = "x-cookie"
        private const val SET_COOKIE = "set-cookie"

        private const val EPK_ID = "epk_id"
        private const val X_EPK_ID = "x-epk-id"
        private const val CLIENT_EPK_ID = "client-epk-id"
    }
}
