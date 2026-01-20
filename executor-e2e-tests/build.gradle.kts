plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Integration tests"

val appProject = project(":executor-application")

dependencies {
    implementation(appProject.the<SourceSetContainer>()["main"].output)
    implementation(appProject)
    implementation(project(":executor-services:voice-executor"))
    implementation(project(":executor-libraries:context"))

    testImplementation(libs.bundles.grpc)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.logging.jvm)
    testImplementation(libs.wiremock.spring.boot)
    implementation(libs.bundles.ktor.client)
}
