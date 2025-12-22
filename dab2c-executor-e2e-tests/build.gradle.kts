plugins {
    id("ru.sbrf.ufs.dab2c.core.java-conventions")
}

description = "Integration tests"

val appProject = project(":dab2c-executor-application")

dependencies {

    implementation(appProject.the<SourceSetContainer>()["main"].output)
    implementation(appProject)

    testImplementation(libs.bundles.testing)
}
