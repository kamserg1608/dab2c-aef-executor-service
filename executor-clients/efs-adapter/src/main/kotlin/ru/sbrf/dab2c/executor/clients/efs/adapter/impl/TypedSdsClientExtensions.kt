package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.mapper.DaSessionInfoMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.SdsSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.SdsSection
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Read single typed value from SDS.
 */
suspend fun <T : Any> SdsClient.read(
    sectionName: String,
    attributeName: String,
    type: Class<T>
): T? {
    val result = readData(listOf(SdsSection(sectionName, attributeName)))
    val data = result.firstOrNull()?.data ?: return null
    return ObjectMappers.MAPPER.readValue(data, type)
}

/**
 * Read single typed value from SDS using reified type.
 */
suspend inline fun <reified T : Any> SdsClient.read(
    sectionName: String,
    attributeName: String
): T? = read(sectionName, attributeName, T::class.java)

/**
 * Write single typed value to SDS.
 */
suspend fun <T : Any> SdsClient.write(
    sectionName: String,
    attributeName: String,
    data: T
) {
    val serialized = ObjectMappers.MAPPER.writeValueAsString(data)
    writeData(listOf(SdsSection(sectionName, attributeName, serialized)))
}

/**
 * Write multiple values to SDS in a single call.
 */
suspend fun SdsClient.writeAll(
    sections: List<Triple<String, String, Any>>
) {
    val sdsSections = sections.map { (sectionName, attributeName, data) ->
        SdsSection(sectionName, attributeName, ObjectMappers.MAPPER.writeValueAsString(data))
    }
    writeData(sdsSections)
}

private const val DA_SESSION_SECTION_NAME = "DA_SESSION"
private const val SESSION_INFO_KEY_PREFIX = "SESSION_INFO"

/**
 * Read DA session info from SDS.
 * @throws IllegalStateException if session info is not found (invalid session)
 */
suspend fun SdsClient.readDaSessionMeta(channel: String): DaSessionMeta {
    val attributeName = "${SESSION_INFO_KEY_PREFIX}_${channel.uppercase()}"
    val sdsSessionMeta = read<SdsSessionMeta>(
        sectionName = DA_SESSION_SECTION_NAME,
        attributeName = attributeName
    ) ?: error("Failed to fetch DA session info: session is invalid or not found")
    return DaSessionInfoMapper.INSTANCE.toDomain(sdsSessionMeta)
}
