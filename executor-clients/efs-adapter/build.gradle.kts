plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
    id("ru.sbrf.ufs.dab2c.core.konvert-conventions")
}

dependencies {
    api(project(":executor-domain-model"))
    implementation(libs.bundles.ktor.client)
    implementation(libs.kotlin.logging.jvm)
}

val generateEfsAdapter = tasks
    .register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateEfsAdapter") {
        configureOpenApi(
            specFile = "$projectDir/src/main/resources/openapi/efs-adapter.yaml",
            basePackage = "ru.sbrf.dab2c.executor.clients.efs.adapter"
        )
    }

tasks.named("compileKotlin") {
    dependsOn(generateEfsAdapter)
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
    dependsOn(generateEfsAdapter)
}