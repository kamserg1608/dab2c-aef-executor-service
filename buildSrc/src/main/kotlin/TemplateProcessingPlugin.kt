import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.SourceSetContainer

/**
 * Convention plugin for processing templates with placeholders using Gradle's Copy task.
 */
class TemplateProcessingPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Ensure Java plugin is applied for processResources task
        project.plugins.apply("java")

        // Create extension
        val extension = project.extensions.create(
            "templateProcessing",
            TemplateProcessingExtension::class.java
        )

        // After evaluation, create Copy tasks for each template configuration and replacement
        project.afterEvaluate {
            val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
            val mainSourceSet = sourceSets.getByName("main")
            val resourcesDir = mainSourceSet.resources.srcDirs.first()

            extension.templates.forEach { templateConfig ->
                val baseTaskName = "process${templateConfig.getName().capitalize()}Template"

                // Create a task that aggregates all copy tasks for this template
                val aggregateTask = project.tasks.register(baseTaskName) {
                    group = "template processing"
                    description = "Processes ${templateConfig.getName()} template with configured replacements"
                }

                // Create a Copy task for each replacement
                templateConfig.replacements.get().forEachIndexed { index, replacement ->
                    if (replacement.name.isBlank()) {
                        throw IllegalArgumentException("Replacement name cannot be blank")
                    }

                    val copyTaskName = "${baseTaskName}${replacement.name.capitalize()}"

                    val copyTask = project.tasks.register(copyTaskName, Copy::class.java) {
                        group = "template processing"
                        description = "Processes ${templateConfig.getName()} template for ${replacement.name}"

                        // Source: the template file
                        from(resourcesDir.resolve(templateConfig.templatePath.get()).parentFile) {
                            include(resourcesDir.resolve(templateConfig.templatePath.get()).name)

                            // Build expansion map: name + all placeholders
                            val expansionMap = mutableMapOf<String, Any>("name" to replacement.name)
                            expansionMap.putAll(replacement.placeholders)

                            // Use Gradle's built-in expand for ${variable} replacement
                            expand(expansionMap)

                            // Rename the output file based on the pattern
                            rename {
                                templateConfig.outputFilePattern.get().replace("{name}", replacement.name)
                            }
                        }

                        // Destination: the output directory
                        into(project.layout.buildDirectory.dir(templateConfig.outputDir.get()))

                        doLast {
                            logger.lifecycle("Generated: ${templateConfig.outputFilePattern.get().replace("{name}", replacement.name)}")
                        }
                    }

                    // Make the aggregate task depend on this copy task
                    aggregateTask.configure {
                        dependsOn(copyTask)
                    }
                }

                // Log summary after all copies complete
                aggregateTask.configure {
                    doLast {
                        val templateFile = resourcesDir.resolve(templateConfig.templatePath.get())
                        val outputDir = project.layout.buildDirectory.dir(templateConfig.outputDir.get()).get().asFile

                        logger.lifecycle("Processing template: ${templateFile.name}")
                        logger.lifecycle("Output directory: ${outputDir.absolutePath}")
                        logger.lifecycle("Successfully processed ${templateConfig.replacements.get().size} replacement(s)")
                    }
                }

                // Make processResources depend on the aggregate task
                project.tasks.named("processResources") {
                    dependsOn(aggregateTask)
                }
            }
        }
    }
}

// Extension function for string capitalization
private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
