import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.Serializable
import javax.inject.Inject

/**
 * Configuration for a single template processing.
 */
abstract class TemplateConfig @Inject constructor(
    private val name: String
) {
    /**
     * Name of this template configuration (used for task naming).
     */
    fun getName(): String = name

    /**
     * Path to the template file relative to resources directory.
     * Example: "templates/DestinationRuleTemplate.yaml"
     */
    abstract val templatePath: Property<String>

    /**
     * Output directory for processed files (relative to build directory).
     * Example: "generated-manifests/destination-rules"
     */
    abstract val outputDir: Property<String>

    /**
     * Output file name pattern. Use {name} placeholder for the config name.
     * Example: "{name}DestinationRule.yaml"
     */
    abstract val outputFilePattern: Property<String>

    /**
     * Set of replacement configurations.
     */
    abstract val replacements: SetProperty<ReplacementConfig>

    /**
     * Add a replacement configuration.
     */
    fun replacement(action: Action<ReplacementConfig>) {
        val replacement = ReplacementConfig()
        action.execute(replacement)
        replacements.add(replacement)
    }

}

/**
 * Configuration for a single replacement (one output file).
 */
data class ReplacementConfig(
    /**
     * Name identifier for this replacement (used in output filename).
     */
    var name: String = "",

    /**
     * Map of placeholder names to their replacement values.
     * Example: mapOf("from" to "serviceAValue", "to" to "serviceBValue")
     */
    val placeholders: MutableMap<String, String> = mutableMapOf()
) : Serializable {
    /**
     * Add a placeholder replacement.
     */
    fun placeholder(key: String, value: String) {
        placeholders[key] = value
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/**
 * Extension for configuring template processing.
 */
abstract class TemplateProcessingExtension @Inject constructor(
    objects: ObjectFactory
) {
    /**
     * Container of template configurations.
     */
    val templates: NamedDomainObjectContainer<TemplateConfig> =
        objects.domainObjectContainer(TemplateConfig::class.java)

    /**
     * Configure a template.
     */
    fun template(name: String, action: Action<TemplateConfig>) {
        templates.create(name, action)
    }
}
