package ru.sbrf.dab2c.executor.clients.giga.agent.audit

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import ru.sbrf.dab2c.executor.clients.giga.agent.api.GigaVoiceAgentClient
import ru.sbrf.dab2c.executor.clients.giga.agent.model.FunctionCallResult
import ru.sbrf.dab2c.executor.clients.giga.agent.model.SettingsResult
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.AudioSettings
import ru.sbrf.dab2c.executor.domain.voice.ContextData
import ru.sbrf.dab2c.executor.domain.voice.FunctionCall
import ru.sbrf.dab2c.executor.domain.voice.FunctionCallingData
import ru.sbrf.dab2c.executor.domain.voice.FunctionPerformers
import ru.sbrf.dab2c.executor.domain.voice.FunctionResultData
import ru.sbrf.dab2c.executor.domain.voice.VoiceSettings
import ru.sbrf.dab2c.executor.library.audit.model.InteractionAuditRequest
import ru.sbrf.dab2c.executor.library.audit.port.InteractionAuditor
import ru.sbrf.dab2c.executor.library.context.Headers
import ru.sbrf.dab2c.executor.library.context.HeadersElement
import ru.sbrf.dab2c.executor.library.context.SessionInfoElement
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers

/**
 * Verifies that [AuditedGigaVoiceAgentClientDecorator] correctly audits
 * success and failure for both getSettings and executeFunctionCall operations.
 */
class AuditedGigaVoiceAgentClientDecoratorTest {

    private val delegate: GigaVoiceAgentClient = mockk()
    private val auditor: InteractionAuditor = mockk(relaxed = true)
    private val objectMapper = ObjectMappers.MAPPER
    private val receiver = "test-receiver"

    private val decorator = AuditedGigaVoiceAgentClientDecorator(delegate, auditor, objectMapper, receiver)

    private val headersElement = HeadersElement(
        Headers(
            mapOf(
                "x-session" to "test-session",
                "x-token" to "test-token",
                "x-eduid" to "test-edu-id",
                "x-channel" to "test-channel",
                "x-platform" to "test-platform"
            )
        )
    )

    private val sessionInfoElement = SessionInfoElement(
        DaSessionInfo(
            meta = DaSessionMeta(
                sessionId = "test-session-id",
                userId = "test-user-id",
                ucpId = "test-ucp-id",
                ufsHost = "test-host"
            ),
            common = DaSessionCommon(channel = "test-channel"),
            userInfo = DaSessionUserInfo()
        )
    )

    private val conversationId = "test-conversation-id"

    private val agentConfiguration = AgentConfiguration(
        name = "test-agent",
        type = "voice",
        functionalSubsystemCi = "test-ci",
        description = "Test agent",
        entryPoints = emptyList(),
        ufsServiceAvailable = true,
        canAccessUserInfo = true,
        toolsMeta = emptyList(),
        neighboursAgentMeta = emptyList(),
        toggles = emptyMap()
    )

    private val voiceSettings = VoiceSettings(
        voiceCallId = "call-123",
        audio = AudioSettings()
    )

    private val contextData = ContextData(content = "{}")

    private val functionCalling = FunctionCallingData(
        functionCall = FunctionCall(name = "get_balance", arguments = """{"id":"1"}"""),
        timestamp = 1000L
    )

    @Nested
    inner class GetSettingsTest {

        private val settingsResult = SettingsResult(
            settings = voiceSettings,
            performers = FunctionPerformers(emptyMap())
        )

        @Test
        fun `should audit success with structured rqMessage and rsMessage on getSettings`() = runTest {
            coEvery {
                delegate.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
            } returns settingsResult

            val auditSlot = slot<InteractionAuditRequest>()

            val result = withContext(headersElement + sessionInfoElement) {
                decorator.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
            }

            assertThat(result).isEqualTo(settingsResult)

            coVerify(exactly = 1) { auditor.success(capture(auditSlot)) }

            val captured = auditSlot.captured
            assertThat(captured.answerCode).isEqualTo("200")
            assertThat(captured.rsMessage).isNotBlank()

            val rqMap = objectMapper.readValue<Map<String, Any?>>(captured.rqMessage!!)
            assertThat(rqMap).containsKey("endpoint")
            assertThat(rqMap["endpoint"]).isEqualTo("/settings")
            assertThat(rqMap).containsKey("receiver")
            assertThat(rqMap["receiver"]).isEqualTo(receiver)
            assertThat(rqMap).containsKey("conversationId")
            assertThat(rqMap["conversationId"]).isEqualTo(conversationId)
            assertThat(rqMap).containsKey("eduId")
            assertThat(rqMap["eduId"]).isEqualTo("test-edu-id")
            assertThat(rqMap).containsKey("ufsSession")
            assertThat(rqMap["ufsSession"]).isEqualTo("test-session")
            assertThat(rqMap).containsKey("channel")
            assertThat(rqMap["channel"]).isEqualTo("test-channel")
            assertThat(rqMap).containsKey("agentConfiguration")
            assertThat(rqMap).containsKey("voiceSettings")
            assertThat(rqMap).containsKey("contextData")
        }

        @Test
        fun `should audit failure and re-throw on getSettings error`() = runTest {
            coEvery {
                delegate.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
            } throws RuntimeException("settings error")

            val auditSlot = slot<InteractionAuditRequest>()

            var thrown: Throwable? = null
            try {
                withContext(headersElement + sessionInfoElement) {
                    decorator.getSettings(conversationId, agentConfiguration, voiceSettings, contextData)
                }
            } catch (e: RuntimeException) {
                thrown = e
            }

            assertThat(thrown).isNotNull()
            assertThat(thrown!!.message).isEqualTo("settings error")

            coVerify(exactly = 1) { auditor.failed(capture(auditSlot)) }

            val captured = auditSlot.captured
            assertThat(captured.answerCode).isEqualTo("500")
            assertThat(captured.errorCode).isEqualTo("GIGAVOICE_SETTINGS_ERROR")
            assertThat(captured.errorTitle).isEqualTo("settings error")
            assertThat(captured.rqMessage).isNotBlank()
            assertThat(captured.rsMessage).isNull()
        }
    }

    @Nested
    inner class ExecuteFunctionCallTest {

        private val functionCallResult = FunctionCallResult(
            result = FunctionResultData(content = """{"balance":1000}""", functionName = "get_balance")
        )

        @Test
        fun `should audit success with structured rqMessage on executeFunctionCall`() = runTest {
            coEvery {
                delegate.executeFunctionCall(conversationId, agentConfiguration, functionCalling, contextData)
            } returns functionCallResult

            val auditSlot = slot<InteractionAuditRequest>()

            val result = withContext(headersElement + sessionInfoElement) {
                decorator.executeFunctionCall(conversationId, agentConfiguration, functionCalling, contextData)
            }

            assertThat(result).isEqualTo(functionCallResult)

            coVerify(exactly = 1) { auditor.success(capture(auditSlot)) }

            val captured = auditSlot.captured
            assertThat(captured.answerCode).isEqualTo("200")
            assertThat(captured.rsMessage).isNotBlank()

            val rqMap = objectMapper.readValue<Map<String, Any?>>(captured.rqMessage!!)
            assertThat(rqMap["endpoint"]).isEqualTo("/functions")
            assertThat(rqMap["receiver"]).isEqualTo(receiver)
            assertThat(rqMap["conversationId"]).isEqualTo(conversationId)
            assertThat(rqMap["eduId"]).isEqualTo("test-edu-id")
            assertThat(rqMap["ufsSession"]).isEqualTo("test-session")
            assertThat(rqMap["channel"]).isEqualTo("test-channel")
            assertThat(rqMap).containsKey("agentConfiguration")
            assertThat(rqMap).containsKey("functionCalling")
            assertThat(rqMap).containsKey("contextData")
        }

        @Test
        fun `should audit failure and re-throw on executeFunctionCall error`() = runTest {
            coEvery {
                delegate.executeFunctionCall(conversationId, agentConfiguration, functionCalling, contextData)
            } throws RuntimeException("function error")

            val auditSlot = slot<InteractionAuditRequest>()

            var thrown: Throwable? = null
            try {
                withContext(headersElement + sessionInfoElement) {
                    decorator.executeFunctionCall(
                        conversationId, agentConfiguration, functionCalling, contextData
                    )
                }
            } catch (e: RuntimeException) {
                thrown = e
            }

            assertThat(thrown).isNotNull()
            assertThat(thrown!!.message).isEqualTo("function error")

            coVerify(exactly = 1) { auditor.failed(capture(auditSlot)) }

            val captured = auditSlot.captured
            assertThat(captured.answerCode).isEqualTo("500")
            assertThat(captured.errorCode).isEqualTo("GIGAVOICE_FUNCTION_ERROR")
            assertThat(captured.errorTitle).isEqualTo("function error")
            assertThat(captured.rqMessage).isNotBlank()
            assertThat(captured.rsMessage).isNull()
        }
    }
}
