@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionCommon
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionInfo
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionMeta
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionUserInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

/**
 * Mapper for converting SDS session models to domain models.
 */
@Konverter
interface DaSessionInfoMapper {

    fun toDomain(source: SdsSessionInfo): DaSessionInfo

    fun toDomain(source: SdsSessionMeta): DaSessionMeta

    fun toDomain(source: SdsSessionCommon): DaSessionCommon

    fun toDomain(source: SdsSessionUserInfo): DaSessionUserInfo

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: DaSessionInfoMapper get() = DaSessionInfoMapperImpl
    }
}
