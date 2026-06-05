plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Integration tests"

val appProject = project(":executor-application")

dependencies {
    implementation(appProject.the<SourceSetContainer>()["main"].output)
    implementation(appProject)
    implementation(project(":executor-services:voice-executor"))
    implementation(project(":executor-libraries:monitoring"))
    implementation(project(":executor-clients:kap-producer"))
    implementation(project(":executor-clients:http-client-factory"))

    testImplementation(libs.bundles.grpc)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.logging.jvm)
    testImplementation(project(":executor-libraries:jackson"))
    testImplementation(project(":executor-libraries:test-support"))
    testImplementation(libs.wiremock.spring.boot)
    testImplementation(libs.spring.kafka)
    testImplementation(libs.spring.kafka.test)
    implementation(libs.bundles.ktor.client)
}
