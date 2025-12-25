plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
}

val generateGigaVoiceAgent = tasks
    .register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateGigaVoiceAgent") {
        configureOpenApi(
            specFile = "$projectDir/src/main/resources/openapi/agent-open-api.yml",
            basePackage = "ru.sbrf.dab2c.executor.clients.giga.agent",
            shouldValidateSpec = false
        )
    }

tasks.named("compileKotlin") {
    dependsOn(generateGigaVoiceAgent)
}
