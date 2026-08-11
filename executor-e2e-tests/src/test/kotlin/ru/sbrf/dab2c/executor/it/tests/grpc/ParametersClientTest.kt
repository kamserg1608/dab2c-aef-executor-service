package ru.sbrf.dab2c.executor.it.tests.grpc

import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.sbrf.dab2c.executor.clients.efs.adapter.api.EfsFacade
import ru.sbrf.dab2c.executor.it.support.runItTest
import ru.sbrf.dab2c.executor.it.support.wiremock.WireMockSetup.stubRetrieveParams
import ru.sbrf.dab2c.executor.it.tests.BaseGigaVoiceIntegrationTest
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement

/**
 * Integration tests for ParametersClient (/retrieveParams endpoint).
 */
class ParametersClientTest : BaseGigaVoiceIntegrationTest() {

    @Autowired
    private lateinit var efsFacade: EfsFacade

    private suspend fun <T> withTestContext(block: suspend () -> T): T {
        val headers = Headers(
            mapOf(
                "x-session" to "test-session",
                "x-token" to "test-token",
                "x-channel" to "test-channel",
                "x-platform" to "test-platform"
            )
        )
        return withContext(HeadersElement(headers)) { block() }
    }

    @Test
    fun `should return string parameter value`() = runItTest {
        efsAdapterMock.stubRetrieveParams("testParam" to "testValue")

        withTestContext {
            val result = efsFacade.getParameter("testParam")
            assertThat(result.name).isEqualTo("testParam")
            assertThat(result.getString()).isEqualTo("testValue")
        }
    }

    @Test
    fun `should return null value for empty parameter type`() = runItTest {
        efsAdapterMock.stubRetrieveParams("testParam" to null)

        withTestContext {
            val result = efsFacade.getParameter("testParam")
            assertThat(result.name).isEqualTo("testParam")
            assertThat(result.value).isNull()
        }
    }

    @Test
    fun `should return boolean parameter`() = runItTest {
        efsAdapterMock.stubRetrieveParams("boolParam" to "true")

        withTestContext {
            val result = efsFacade.getParameter("boolParam")
            assertThat(result.getBool()).isTrue()
        }
    }

    @Test
    fun `should return long parameter`() = runItTest {
        efsAdapterMock.stubRetrieveParams("longParam" to "42")

        withTestContext {
            val result = efsFacade.getParameter("longParam")
            assertThat(result.getLong()).isEqualTo(42L)
        }
    }

    @Test
    fun `should return double parameter`() = runItTest {
        efsAdapterMock.stubRetrieveParams("doubleParam" to "3.14")

        withTestContext {
            val result = efsFacade.getParameter("doubleParam")
            assertThat(result.getDouble()).isEqualTo(3.14)
        }
    }

    @Test
    fun `should throw when accessing empty parameter without default`() = runItTest {
        efsAdapterMock.stubRetrieveParams("testParam" to null)

        withTestContext {
            val result = efsFacade.getParameter("testParam")
            assertThatThrownBy { result.getString() }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("testParam")
        }
    }

    @Test
    fun `should return default value for empty parameter`() = runItTest {
        efsAdapterMock.stubRetrieveParams("testParam" to null)

        withTestContext {
            val result = efsFacade.getParameter("testParam")
            assertThat(result.getString { "fallback" }).isEqualTo("fallback")
            assertThat(result.getBool { false }).isFalse()
            assertThat(result.getLong { -1L }).isEqualTo(-1L)
            assertThat(result.getDouble { 0.0 }).isEqualTo(0.0)
        }
    }

    @Test
    fun `should retrieve multiple parameters in single call`() = runItTest {
        efsAdapterMock.stubRetrieveParams(
            "param1" to "value1",
            "param2" to "true",
            "param3" to "42"
        )

        withTestContext {
            val result = efsFacade.getParameters(listOf("param1", "param2", "param3"))
            assertThat(result).hasSize(3)
            assertThat(result["param1"]?.getString()).isEqualTo("value1")
            assertThat(result["param2"]?.getBool()).isTrue()
            assertThat(result["param3"]?.getLong()).isEqualTo(42L)
        }
    }

    @Test
    fun `should return null value for empty parameters in batch result`() = runItTest {
        efsAdapterMock.stubRetrieveParams("testParam" to null)

        withTestContext {
            val result = efsFacade.getParameters(listOf("testParam"))
            assertThat(result).hasSize(1)
            assertThat(result["testParam"]?.value).isNull()
        }
    }

    @Test
    fun `should throw on server error`() {
        assertThatThrownBy {
            runItTest {
                efsAdapterMock.stubRetrieveParams("""{"success":false,"body":null}""")

                withTestContext {
                    efsFacade.getParameter("testParam")
                }
            }
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("success=false")
    }
}
