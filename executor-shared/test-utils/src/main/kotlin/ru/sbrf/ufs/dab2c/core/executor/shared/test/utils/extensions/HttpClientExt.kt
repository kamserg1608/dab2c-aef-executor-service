package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.extensions

import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.springframework.http.MediaType
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.json.JsonUtil

fun HttpClient.execute(
    url: String,
    requestBody: Any,
    headers: Map<String, String?> = emptyMap(),
): HttpResponse {
    val request = HttpPost(url)
    request.entity = StringEntity(JsonUtil.serialize(requestBody))
        .apply { setContentType(MediaType.APPLICATION_JSON_VALUE) }
    headers.forEach { (key, value) -> request.addHeader(key, value) }
    return execute(request)
}
