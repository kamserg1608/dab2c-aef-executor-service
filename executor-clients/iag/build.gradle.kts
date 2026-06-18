plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
}

val generateIagApi = tasks
    .register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateIagApi") {
        configureOpenApi(
            specFile = "$projectDir/src/main/resources/openapi/iag-open-api.json",
            basePackage = "ru.sbrf.dab2c.executor.clients.iag.generated",
            shouldValidateSpec = false
        )

        importMappings.set(
            mapOf(
                "ACLConfig" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig",
                "UserInfo" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.UserInfo",
                "SessionInfo" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionInfo"
            )
        )

        schemaMappings.set(
            mapOf(
                "ACLConfig" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.ACLConfig",
                "UserInfo" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.UserInfo",
                "SessionInfo" to "ru.sbrf.dab2c.executor.clients.giga.agent.model.SessionInfo"
            )
        )
    }

tasks.named("compileKotlin") {
    dependsOn(generateIagApi)
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:context"))
    implementation(project(":executor-clients:http-client-factory"))
    implementation(project(":executor-libraries:monitoring"))
    implementation(project(":executor-libraries:jackson"))
    implementation(project(":executor-libraries:logging"))
    implementation(project(":executor-clients:giga-agent"))
    implementation(project(":executor-clients:giga-voice"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.logging.jvm)
}
