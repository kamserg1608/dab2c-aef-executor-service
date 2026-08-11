plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
    id("ru.sbrf.ufs.dab2c.core.konvert-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:context"))
    implementation(project(":executor-clients:http-client-factory"))
    implementation(project(":executor-libraries:monitoring"))
    implementation(project(":executor-libraries:jackson"))
    implementation(project(":executor-libraries:logging"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.logging.jvm)
}

val generateConfiguratorApi = tasks
    .register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateConfiguratorApi") {
        configureOpenApi(
            specFile = "$projectDir/src/main/resources/openapi/configurator.yaml",
            basePackage = "ru.sbrf.dab2c.executor.clients.configurator"
        )
    }

tasks.named("compileKotlin") {
    dependsOn(generateConfiguratorApi)
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateConfiguratorApi)
}
