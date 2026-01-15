package ru.sbrf.dab2c.executor.clients.efs.adapter.api

/**
 * Type-safe SDS client that handles serialization/deserialization.
 */
interface TypedSdsClient {

    /**
     * Read single typed value from SDS.
     */
    suspend fun <T : Any> read(
        sectionName: String,
        attributeName: String,
        cookie: String,
        type: Class<T>
    ): T?

    /**
     * Write single typed value to SDS.
     */
    suspend fun <T : Any> write(
        sectionName: String,
        attributeName: String,
        data: T,
        cookie: String
    )

    /**
     * Write multiple values to SDS in a single call.
     */
    suspend fun writeAll(
        sections: List<Triple<String, String, Any>>,
        cookie: String
    )
}
