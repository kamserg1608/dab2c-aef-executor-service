plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Integration tests"

val appProject = project(":executor-application")

dependencies {

    implementation(appProject.the<SourceSetContainer>()["main"].output)
    implementation(appProject)

    implementation(project(":executor-services:voice-executor"))

    testImplementation(libs.bundles.testing)
    testImplementation(libs.bundles.grpc)
    testImplementation(libs.kotlinx.coroutines.test)
}
