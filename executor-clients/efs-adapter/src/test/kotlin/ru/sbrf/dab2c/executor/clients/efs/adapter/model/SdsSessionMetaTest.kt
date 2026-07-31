package ru.sbrf.dab2c.executor.clients.efs.adapter.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.DaSessionInfoMapper
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

class SdsSessionMetaTest {

    @Test
    fun `should use user id when ucp id is null`() {
        val source = """
            {
              "session_id": "session-id",
              "user_id": "user-id",
              "ucp_id": null,
              "ufs_host": "http://host",
              "is_valid": true
            }
        """.trimIndent()

        val result = ObjectMappers.MAPPER.readValue(source, SdsSessionMeta::class.java)
        val domain = DaSessionInfoMapper.INSTANCE.toDomain(result)

        assertEquals("user-id", result.ucpId)
        assertEquals("user-id", domain.ucpId)
    }

    @Test
    fun `should preserve ucp id when it is present`() {
        val source = """
            {
              "session_id": "session-id",
              "user_id": "user-id",
              "ucp_id": "ucp-id",
              "ufs_host": "http://host",
              "is_valid": true
            }
        """.trimIndent()

        val result = ObjectMappers.MAPPER.readValue(source, SdsSessionMeta::class.java)
        val domain = DaSessionInfoMapper.INSTANCE.toDomain(result)

        assertEquals("ucp-id", result.ucpId)
        assertEquals("ucp-id", domain.ucpId)
    }
}
