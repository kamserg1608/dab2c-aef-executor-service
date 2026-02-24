package ru.sbrf.dab2c.executor.library.context

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo

class SessionInfoElementTest {

    @Test
    fun `should store DaSessionInfo`() {
        val sessionInfo = createDaSessionInfo()
        val element = SessionInfoElement(sessionInfo)

        assertSame(sessionInfo, element.sessionInfo)
    }

    @Test
    fun `should have correct key`() {
        val element = SessionInfoElement(createDaSessionInfo())

        assertEquals(SessionInfoElement.Key, element.key)
    }

    @Test
    fun `should be retrievable by key from coroutine context`() {
        val sessionInfo = createDaSessionInfo()
        val element = SessionInfoElement(sessionInfo)
        val context = element

        val retrieved = context[SessionInfoElement]

        assertSame(sessionInfo, retrieved?.sessionInfo)
    }

    private fun createDaSessionInfo(): DaSessionInfo = DaSessionInfo(
        meta = DaSessionMeta(
            sessionId = "test-session",
            userId = "test-user",
            ucpId = "test-ucp",
            ufsHost = "test-host"
        ),
        common = DaSessionCommon(channel = "test"),
        userInfo = DaSessionUserInfo()
    )
}
