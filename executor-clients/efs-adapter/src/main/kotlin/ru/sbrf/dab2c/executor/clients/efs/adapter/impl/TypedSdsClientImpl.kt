package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import com.fasterxml.jackson.databind.ObjectMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.DaSessionInfoMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.SdsSection

/**
 * Implementation of type-safe SDS client using Jackson for serialization.
 */
class TypedSdsClientImpl(
    private val sdsClient: SdsClient,
    private val objectMapper: ObjectMapper
) : TypedSdsClient {

    override suspend fun <T : Any> read(
        sectionName: String,
        attributeName: String,
        type: Class<T>
    ): T? {
        val result = sdsClient.readData(
            listOf(SdsSection(sectionName, attributeName))
        )
        val data = result.firstOrNull()?.data ?: return null
        return objectMapper.readValue(data, type)
    }

    override suspend fun <T : Any> write(
        sectionName: String,
        attributeName: String,
        data: T
    ) {
        val serialized = objectMapper.writeValueAsString(data)
        sdsClient.writeData(
            listOf(SdsSection(sectionName, attributeName, serialized))
        )
    }

    override suspend fun writeAll(
        sections: List<Triple<String, String, Any>>
    ) {
        val sdsSections = sections.map { (sectionName, attributeName, data) ->
            SdsSection(sectionName, attributeName, objectMapper.writeValueAsString(data))
        }
        sdsClient.writeData(sdsSections)
    }
}

/**
 * Read single typed value from SDS.
 */
suspend inline fun <reified T : Any> TypedSdsClient.read(
    sectionName: String,
    attributeName: String
): T? = read(sectionName, attributeName, T::class.java)

private const val DA_SESSION_SECTION_NAME = "DA_SESSION"
private const val SESSION_INFO_KEY_PREFIX = "SESSION_INFO"

/**
 * Read DA session info from SDS.
 * @throws IllegalStateException if session info is not found (invalid session)
 */
suspend fun TypedSdsClient.readDaSessionMeta(channel: String): DaSessionMeta {
    val attributeName = "${SESSION_INFO_KEY_PREFIX}_${channel.uppercase()}"
    val sdsSessionMeta = read<SdsSessionMeta>(
        sectionName = DA_SESSION_SECTION_NAME,
        attributeName = attributeName
    ) ?: error("Failed to fetch DA session info: session is invalid or not found")
    return DaSessionInfoMapper.INSTANCE.toDomain(sdsSessionMeta)
}
