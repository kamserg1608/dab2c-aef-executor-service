package ru.sbrf.dab2c.executor.domain.configuration

/**
 * Agent configuration from EFS adapter.
 */
data class AgentConfiguration(
    val name: String,
    val type: String,
    val functionalSubsystemCi: String,
    val description: String,
    val entryPoints: List<String>,
    val ufsServiceAvailable: Boolean,
    val canAccessUserInfo: Boolean,
    val toolsMeta: List<ToolMeta>,
    val neighboursAgentMeta: List<NeighbourAgentMeta>,
    val toggles: Map<String, Boolean>
)

/**
 * Tool metadata for agent configuration.
 */
data class ToolMeta(
    val name: String,
    val description: String,
    val parameters: ToolParameters?,
    val fewShotExamples: List<FewShotExample>,
    val returnParameters: ReturnParameters?,
    val xType: ToolType?,
    val xVersion: Int?,
    val xPayload: String?
)

/**
 * Tool type enum.
 */
enum class ToolType {
    FUNCTION,
    WIDGET
}

/**
 * Tool parameters schema.
 */
data class ToolParameters(
    val type: String,
    val properties: Map<String, ToolParameterProperty>,
    val required: List<String>
)

/**
 * Tool parameter property definition.
 */
data class ToolParameterProperty(
    val type: String,
    val enumValues: List<String>,
    val description: String
)

/**
 * Few-shot example for tool.
 */
data class FewShotExample(
    val request: String,
    val params: Map<String, Any?>
)

/**
 * Return parameters schema.
 */
data class ReturnParameters(
    val type: String,
    val properties: Map<String, ReturnParameter>
)

/**
 * Return parameter definition.
 */
data class ReturnParameter(
    val type: String,
    val format: String?,
    val description: String
)

/**
 * Neighbour agent metadata.
 */
data class NeighbourAgentMeta(
    val name: String,
    val type: String,
    val description: String,
    val functionalSubsystemCi: String,
    val toolsMeta: List<ToolMeta>
)
