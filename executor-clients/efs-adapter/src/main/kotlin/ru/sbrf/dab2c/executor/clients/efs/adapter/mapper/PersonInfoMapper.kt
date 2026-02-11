package ru.sbrf.dab2c.executor.clients.efs.adapter.mapper

import ru.sbrf.dab2c.executor.clients.efs.adapter.impl.PersonInfoMapperImpl
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AdditionalInfo
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.PersonType
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

/**
 * Mapper for PersonInfo.
 */
interface PersonInfoMapper {

    /** Maps [PersonType] to [DaSessionUserInfo]. */
    fun toDomain(source: PersonType?, additionalInfo: AdditionalInfo?): DaSessionUserInfo

    /** Provides singleton instance of the mapper. */
    companion object {
        val INSTANCE: PersonInfoMapper get() = PersonInfoMapperImpl()
    }
}
