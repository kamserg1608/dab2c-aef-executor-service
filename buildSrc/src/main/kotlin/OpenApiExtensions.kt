import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

fun GenerateTask.configureOpenApi(
    specFile: String,
    basePackage: String,
    shouldValidateSpec: Boolean = false
) {
    generatorName.set("kotlin")
    inputSpec.set(specFile)
    outputDir.set("${project.layout.buildDirectory.get()}/generated/openapi")
    packageName.set(basePackage)
    modelPackage.set("$basePackage.model")
    apiPackage.set("$basePackage.api")
    configOptions.set(mapOf(
        "library" to "jvm-ktor",
        "dateLibrary" to "java8",
        "serializationLibrary" to "jackson",
        "enumPropertyNaming" to "UPPERCASE",
        "modelPropertyNaming" to "camelCase",
        "useCoroutines" to "true"
    ))
    generateModelTests.set(false)
    generateApiTests.set(false)
    generateModelDocumentation.set(false)
    generateApiDocumentation.set(false)
    globalProperties.set(mapOf(
        "models" to "",
        "apis" to "false",
        "supportingFiles" to "false",
        "modelTests" to "false",
        "modelDocs" to "false"
    ))
    validateSpec.set(shouldValidateSpec)
}