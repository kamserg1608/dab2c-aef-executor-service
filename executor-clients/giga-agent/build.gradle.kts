plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
}

dependencies {
    implementation(libs.bundles.ktor.client)
}

val generateGigaVoiceAgent = tasks
    .register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateGigaVoiceAgent") {
        configureOpenApi(
            specFile = "$projectDir/src/main/resources/openapi/agent-open-api.json",
            basePackage = "ru.sbrf.dab2c.executor.clients.giga.agent",
            shouldValidateSpec = false
        )
    }

tasks.named("compileKotlin") {
    dependsOn(generateGigaVoiceAgent)
}
