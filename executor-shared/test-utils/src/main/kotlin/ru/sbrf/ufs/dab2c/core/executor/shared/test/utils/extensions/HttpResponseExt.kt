package ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.extensions

import org.apache.http.HttpResponse
import org.apache.http.util.EntityUtils
import org.assertj.core.api.Assertions
import org.springframework.http.HttpStatus
import ru.sbrf.ufs.dab2c.core.executor.shared.test.utils.json.JsonUtil

fun HttpResponse.assertHasSameEntityAsResource(pathToResource: String) {
    val responseBody = EntityUtils.toString(this.entity)
    Assertions.assertThat(JsonUtil.readTree(responseBody)).isEqualTo(JsonUtil.load(pathToResource))
}

fun HttpResponse.assertHasSameEntity(expected: Any) {
    val responseBody = EntityUtils.toString(this.entity)
    Assertions.assertThat(JsonUtil.readTree(responseBody)).isEqualTo(JsonUtil.valueToTree(expected))
}

fun HttpResponse.assertHasStatus(httpStatus: HttpStatus) {
    Assertions.assertThat(statusLine.statusCode).isEqualTo(httpStatus.value())
}
