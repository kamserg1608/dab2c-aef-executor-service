package ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.integration.helpers

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpUriRequest
import java.io.IOException

/**
 * Helper class for executing the request.
 */
class HttpClientHelper(
    private val jsonMapper: ObjectMapper,
    private val client: HttpClient
) {

    @Throws(IOException::class)
    fun <T> execute(request: HttpUriRequest?, clazz: Class<T>?): T {
        val response = client.execute(request)
        return jsonMapper.readValue(response.entity.content, clazz)
    }
}
