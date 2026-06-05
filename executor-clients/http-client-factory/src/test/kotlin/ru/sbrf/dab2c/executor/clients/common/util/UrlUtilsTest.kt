package ru.sbrf.dab2c.executor.clients.common.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UrlUtilsTest {

    @ParameterizedTest(name = "baseUrl=''{0}'' + endpoint=''{1}'' = ''{2}''")
    @CsvSource(
        "http://host:8080, /endpoint, http://host:8080/endpoint",
        "http://host:8080/, /endpoint, http://host:8080/endpoint",
        "http://host:8080, endpoint, http://host:8080/endpoint",
        "http://host:8080/, endpoint, http://host:8080/endpoint",
        "http://host:8080/api/v1, /session/readData, http://host:8080/api/v1/session/readData",
        "http://host:8080/api/v1/, /session/readData, http://host:8080/api/v1/session/readData",
        "http://host:8080/api/v1, session/readData, http://host:8080/api/v1/session/readData",
        "http://host:8080/api/v1/, session/readData, http://host:8080/api/v1/session/readData",
        "http://host:8080/da-ufs-sidecar/v1, /settings, http://host:8080/da-ufs-sidecar/v1/settings",
        "https://example.com/path, /endpoint, https://example.com/path/endpoint"
    )
    fun `should correctly combine base URL and endpoint`(
        baseUrl: String,
        endpoint: String,
        expected: String
    ) {
        val result = buildFullUrl(baseUrl, endpoint)
        assertThat(result).isEqualTo(expected)
    }
}
