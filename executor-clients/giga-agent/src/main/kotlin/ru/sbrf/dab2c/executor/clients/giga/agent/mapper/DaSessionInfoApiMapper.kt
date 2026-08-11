@file:Suppress("UndocumentedPublicFunction")

package ru.sbrf.dab2c.executor.clients.giga.agent.mapper

import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionInfo
import ru.sbrf.dab2c.executor.clients.giga.agent.model.UserInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

/**
 * Mapper for converting domain DaSessionInfo to GigaVoice Agent API models.
 */
object DaSessionInfoApiMapper {

    fun toApiSessionInfo(source: DaSessionInfo, context: GigaAgentRequestContext): SessionInfo =
        SessionInfo(
            ufsHost = source.meta.ufsHost,
            cookies = context.toCookiesMap(),
            headers = context.toHeadersMap()
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
