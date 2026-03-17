@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.model.UserInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.RequestHeader

/**
 * Mapper for converting domain DaSessionInfo to GigaVoice Agent API models.
 */
object DaSessionInfoApiMapper {

    fun toApiSessionInfo(source: DaSessionInfo, headers: Headers): SessionInfo =
        SessionInfo(
            ufsHost = source.meta.ufsHost,
            cookies = null,
            headers = headers.headers.filterKeys { it != RequestHeader.TOKEN.headerName }
        )

    fun toApiUserInfo(source: DaSessionInfo): UserInfo =
        toApiUserInfo(source.userInfo)

    fun toApiUserInfo(source: DaSessionUserInfo): UserInfo =
        UserInfo(
            firstName = source.firstName,
            patrName = source.patrName,
            birthDay = source.birthDay,
            segmentCodeType = source.segmentCodeType,
            ucpId = source.ucpId
        )
}
