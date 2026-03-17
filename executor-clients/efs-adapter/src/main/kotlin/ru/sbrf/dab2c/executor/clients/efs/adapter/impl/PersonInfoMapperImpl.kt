package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.PersonInfoMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AdditionalInfo
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.PersonType
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.library.jackson.DateFormatters

/**
 * Custom mapper for PersonInfo.
 */
class PersonInfoMapperImpl : PersonInfoMapper {

    override fun toDomain(source: PersonType?, additionalInfo: AdditionalInfo?): DaSessionUserInfo =
        DaSessionUserInfo(
            firstName = source?.firstName,
            patrName = source?.patrName,
            birthDay = source?.birthDay?.format(DateFormatters.DEFAULT),
            segmentCodeType = additionalInfo?.segmentCodeType,
            ucpId = source?.ucpId
        )
}
