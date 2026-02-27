plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:context"))
    api(project(":executor-libraries:monitoring"))
    api(project(":executor-libraries:audit"))
    implementation(project(":executor-clients:common-converters"))
    implementation(project(":executor-libraries:jackson"))
    implementation(project(":executor-libraries:logging"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.logging.jvm)
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
