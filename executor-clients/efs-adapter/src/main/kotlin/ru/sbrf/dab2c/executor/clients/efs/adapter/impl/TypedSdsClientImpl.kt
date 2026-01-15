package ru.sbrf.dab2c.executor.clients.efs.adapter.impl

import com.fasterxml.jackson.databind.ObjectMapper
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.SdsClient
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.TypedSdsClient
import ru.sbrf.dab2c.executor.domain.sds.SdsSection

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
        cookie: String,
        type: Class<T>
    ): T? {
        val result = sdsClient.readData(
            listOf(SdsSection(sectionName, attributeName)),
            cookie
        )
        val data = result.firstOrNull()?.data ?: return null
        return objectMapper.readValue(data, type)
    }

    override suspend fun <T : Any> write(
        sectionName: String,
        attributeName: String,
        data: T,
        cookie: String
    ) {
        val serialized = objectMapper.writeValueAsString(data)
        sdsClient.writeData(
            listOf(SdsSection(sectionName, attributeName, serialized)),
            cookie
        )
    }

    override suspend fun writeAll(
        sections: List<Triple<String, String, Any>>,
        cookie: String
    ) {
        val sdsSections = sections.map { (sectionName, attributeName, data) ->
            SdsSection(sectionName, attributeName, objectMapper.writeValueAsString(data))
        }
        sdsClient.writeData(sdsSections, cookie)
    }
}

/**
 * Read single typed value from SDS.
 */
suspend inline fun <reified T : Any> TypedSdsClient.read(
    sectionName: String,
    attributeName: String,
    cookie: String
): T? = read(sectionName, attributeName, cookie, T::class.java)
