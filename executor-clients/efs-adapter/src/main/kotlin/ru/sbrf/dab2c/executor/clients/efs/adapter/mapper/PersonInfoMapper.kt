package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import io.mcarle.konvert.api.Konverter
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.PersonType
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

/**
 * Mapper for PersonInfo.
 */
@Konverter
interface PersonInfoMapper {

    /** Maps [PersonType] to [DaSessionUserInfo]. */
    fun toDomain(source: PersonType): DaSessionUserInfo

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: PersonInfoMapper get() = PersonInfoMapperImpl
    }
}
