package ru.sbrf.dab2c.executor.application.warmup

import ru.sbrf.dab2c.executor.clients.configurator.model.BaseResponseFunctionListResponse
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseListSdsSectionData
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringAgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringFunctionConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseParameters
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseProfile
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseSessionConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseVoid
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.Department
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.EmployeeInfo
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.PersonDepartment
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.PersonType
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ProductsExt
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.Profile
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.UkoSettings
import ru.sbrf.dab2c.executor.clients.giga.agent.mapper.GigaVoiceSettingsRequestBuilder
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaAgentRequestContext
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceFunctionsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsRequestSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.GigaVoiceSettingsResponseSchema
import ru.sbrf.dab2c.executor.clients.giga.agent.model.PostProcessContextResponseSchema
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.GigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.Settings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.audioSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.function
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCall
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.functionCalling
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaChatSettings
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceRequest
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.gigaVoiceResponse
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.initialContext
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.input
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.message
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.output
import ru.sbrf.dab2c.executor.clients.gigavoice.proto.settings
import ru.sbrf.dab2c.executor.clients.iag.model.IagFunctionResponse
import ru.sbrf.dab2c.executor.domain.configuration.AgentConfiguration
import ru.sbrf.dab2c.executor.domain.configuration.FewShotExample
import ru.sbrf.dab2c.executor.domain.configuration.NeighbourAgentMeta
import ru.sbrf.dab2c.executor.domain.configuration.ReturnParameter
import ru.sbrf.dab2c.executor.domain.configuration.ReturnParameters
import ru.sbrf.dab2c.executor.domain.configuration.ToolMeta
import ru.sbrf.dab2c.executor.domain.configuration.ToolParameterProperty
import ru.sbrf.dab2c.executor.domain.configuration.ToolParameters
import ru.sbrf.dab2c.executor.domain.configuration.ToolType
import ru.sbrf.dab2c.executor.domain.session.DaSessionCommon
import ru.sbrf.dab2c.executor.domain.session.DaSessionInfo
import ru.sbrf.dab2c.executor.domain.session.DaSessionMeta
import ru.sbrf.dab2c.executor.domain.session.DaSessionUserInfo
import ru.sbrf.dab2c.executor.domain.voice.DialogContext
import ru.sbrf.dab2c.executor.library.jackson.ObjectMappers.MAPPER
import java.time.OffsetDateTime

/** Nested fields of a serialized payload must stay non-null, otherwise the branch is not warmed. */
internal object WarmupPayloads {

    val DESERIALIZED_TYPES: List<Class<*>> = listOf(
        BaseResponseListSdsSectionData::class.java,
        BaseResponseSessionConfig::class.java,
        BaseResponseProfile::class.java,
        BaseResponseParameters::class.java,
        BaseResponseMapStringAgentConfig::class.java,
        BaseResponseVoid::class.java,
        GigaVoiceSettingsResponseSchema::class.java,
        BaseResponseMapStringFunctionConfig::class.java,
        BaseResponseFunctionListResponse::class.java,
        GigaVoiceFunctionsResponseSchema::class.java,
        PostProcessContextResponseSchema::class.java,
        IagFunctionResponse::class.java
    )

    val SERIALIZED_PAYLOADS: List<Any> = listOf(
        personInfoResponse(),
        settingsRequest(),
        settingsAuditMessage(),
        voiceRequest(),
        voiceResponse()
    )

    private const val SCHEMA_TYPE_OBJECT = "object"
    private const val SCHEMA_TYPE_STRING = "string"
    private const val TOOL_PROPERTY_NAME = "warmup-property"

    private fun personInfoResponse(): BaseResponseProfile =
        BaseResponseProfile(
            success = true,
            body = Profile(
                person = person(),
                products = ProductsExt(),
                ukoSettings = UkoSettings(),
                employeeInfo = EmployeeInfo(
                    login = "warmup-login",
                    firstName = "warmup-employee-first-name",
                    lastName = "warmup-employee-last-name"
                )
            )
        )

    private fun person(): PersonType =
        PersonType(
            firstName = "warmup-first-name",
            patrName = "warmup-patr-name",
            surName = "warmup-sur-name",
            ucpId = "warmup-ucp-id",
            segmentCode = "warmup-segment",
            lastLogonDate = OffsetDateTime.parse("2020-01-01T00:00:00Z"),
            department = Department(name = "warmup-department"),
            personDepartment = PersonDepartment(
                tb = "warmup-tb",
                osb = "warmup-osb",
                vsp = "warmup-vsp",
                name = "warmup-person-department"
            )
        )

    private fun settingsRequest(): GigaVoiceSettingsRequestSchema =
        GigaVoiceSettingsRequestBuilder().build(
            context = requestContext(),
            agentConfiguration = agentConfiguration(),
            voiceSettings = voiceSettings(),
            daSessionInfo = sessionInfo(),
            contextData = dialogContext()
        )

    private fun settingsAuditMessage(): Map<String, Any> =
        mapOf(
            "agentConfiguration" to agentConfiguration(),
            "voiceSettings" to voiceSettings(),
            "contextData" to dialogContext()
        )

    private fun dialogContext(): DialogContext =
        DialogContext(MAPPER.createObjectNode().put("warmupContextKey", "warmupContextValue"))

    private fun requestContext(): GigaAgentRequestContext =
        GigaAgentRequestContext(
            ufsSession = "warmup-session",
            ufsToken = "warmup-token",
            channel = "warmup-channel",
            conversationId = "warmup-conversation",
            eduId = "warmup-edu-id",
            traceId = "warmup-trace-id",
            daRequestId = "warmup-request-id",
            daSessionId = "warmup-session-id",
            daChannel = "warmup-da-channel",
            daPlatform = "warmup-platform",
            daUcpId = "warmup-da-ucp-id"
        )

    private fun sessionInfo(): DaSessionInfo =
        DaSessionInfo(
            meta = DaSessionMeta(
                sessionId = "warmup-meta-session-id",
                userId = "warmup-user-id",
                ucpId = "warmup-meta-ucp-id",
                ufsHost = "warmup-host"
            ),
            common = DaSessionCommon(
                channel = "warmup-common-channel",
                platform = "warmup-common-platform",
                surface = "warmup-surface",
                entryPoint = "warmup-common-entry-point"
            ),
            userInfo = DaSessionUserInfo(
                firstName = "warmup-user-first-name",
                patrName = "warmup-user-patr-name",
                birthDay = "2020-01-01",
                segmentCodeType = "warmup-segment-type",
                ucpId = "warmup-user-ucp-id"
            )
        )

    private fun agentConfiguration(): AgentConfiguration =
        AgentConfiguration(
            name = "warmup-agent",
            type = "warmup-agent-type",
            functionalSubsystemCi = "warmup-ci",
            description = "warmup agent configuration",
            entryPoints = listOf("warmup-entry-point"),
            ufsServiceAvailable = true,
            canAccessUserInfo = true,
            toolsMeta = listOf(toolMeta()),
            neighboursAgentMeta = listOf(neighbourAgentMeta()),
            toggles = mapOf("warmup-toggle" to true)
        )

    private fun neighbourAgentMeta(): NeighbourAgentMeta =
        NeighbourAgentMeta(
            name = "warmup-neighbour",
            type = "warmup-neighbour-type",
            description = "warmup neighbour description",
            functionalSubsystemCi = "warmup-neighbour-ci",
            toolsMeta = listOf(toolMeta())
        )

    private fun toolMeta(): ToolMeta =
        ToolMeta(
            name = "warmup-tool",
            description = "warmup tool description",
            parameters = ToolParameters(
                type = SCHEMA_TYPE_OBJECT,
                properties = mapOf(
                    TOOL_PROPERTY_NAME to ToolParameterProperty(
                        type = SCHEMA_TYPE_STRING,
                        enumValues = listOf("warmup-enum-value"),
                        description = "warmup property description"
                    )
                ),
                required = listOf(TOOL_PROPERTY_NAME)
            ),
            fewShotExamples = listOf(FewShotExample(request = "warmup request", params = mapOf("warmup" to "param"))),
            returnParameters = ReturnParameters(
                type = SCHEMA_TYPE_OBJECT,
                properties = mapOf(
                    "warmup-return" to ReturnParameter(
                        type = SCHEMA_TYPE_STRING,
                        format = "warmup-format",
                        description = "warmup return description"
                    )
                )
            ),
            xType = ToolType.FUNCTION,
            xVersion = 1,
            xPayload = "warmup-payload"
        )

    private fun voiceSettings(): Settings =
        settings {
            voiceCallId = "warmup-call-id"
            mode = Settings.Mode.RECOGNIZE_GIGACHAT_SYNTHESIS
            outputModalities = Settings.OutputModalities.AUDIO_TEXT
            gigachat = gigaChatSettings {
                model = "warmup-model"
                functions.add(
                    function {
                        name = "warmup-function"
                        description = "warmup function description"
                        parameters = """{"type":"object"}"""
                    }
                )
            }
            audio = audioSettings {
                input = input { model = "warmup-input-model" }
                output = output { voice = "warmup-voice" }
            }
            context = initialContext {
                messages.add(
                    message {
                        role = "user"
                        content = "warmup message"
                    }
                )
            }
        }

    private fun voiceRequest(): GigaVoiceRequest =
        gigaVoiceRequest { settings = voiceSettings() }

    private fun voiceResponse(): GigaVoiceResponse =
        gigaVoiceResponse {
            functionCall = functionCalling {
                functionCall = functionCall {
                    name = "warmup-called-function"
                    arguments = """{"warmup":"argument"}"""
                }
            }
        }
}
