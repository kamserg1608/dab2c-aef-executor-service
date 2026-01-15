package ru.sbrf.dab2c.executor.clients.efs.adapter.stub

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.AppSourceRequest
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.BaseResponseMapStringAgentConfig
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ToolMeta
import ru.sbrf.dab2c.executor.clients.efs.adapter.model.ToolParameters
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

private const val STUB_AGENT_NAME = "voice-executor-stub"
private const val OBJECT_TYPE = "object"
private const val AGENT_TYPE_VOICE = "voice"

/**
 * Stub REST controller for EFS Adapter API endpoints.
 * Active only when STUB profile is enabled.
 */
@RestController
@Profile("STUB")
class EfsAdapterStubController {

    private val callCounter = AtomicLong(0)

    /**
     * Returns stub agent configuration for testing purposes.
     */
    @PostMapping("/configurator/rest-agent")
    fun getRestAgentConfig(@RequestBody request: AppSourceRequest): BaseResponseMapStringAgentConfig {
        val callNumber = callCounter.incrementAndGet()
        logger.info { "STUB /configurator/rest-agent called #$callNumber for appSource: ${request.appSource}" }

        return BaseResponseMapStringAgentConfig(
            success = true,
            body = mapOf(STUB_AGENT_NAME to createStubAgentConfig()),
            context = null,
            messages = emptyList(),
            error = null,
            alerts = emptyList()
        )
    }

    private fun createStubAgentConfig(): AgentConfig = AgentConfig(
        name = STUB_AGENT_NAME,
        type = AGENT_TYPE_VOICE,
        functionalSubsystemCi = "stub-ci",
        description = "Stub agent for testing purposes",
        entryPoints = listOf(AGENT_TYPE_VOICE, "ivr"),
        ufsServiceAvailable = true,
        canAccessUserInfo = true,
        toolsMeta = createStubToolsMeta(),
        neighboursAgentMeta = emptyList(),
        toggles = mapOf("enable_backend_functions" to true, "enable_transfer" to true)
    )

    private fun createStubToolsMeta(): List<ToolMeta> = listOf(
        createToolMeta("stub_function", "A stub function for testing"),
        createToolMeta("transfer_to_operator", "Transfer call to operator"),
        createToolMeta("end_dialog", "End the dialog")
    )

    private fun createToolMeta(name: String, description: String): ToolMeta = ToolMeta(
        name = name,
        description = description,
        parameters = ToolParameters(type = OBJECT_TYPE, properties = emptyMap(), required = emptyList())
    )
}
