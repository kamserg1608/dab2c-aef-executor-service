plugins {
    id("ru.sbrf.ufs.dab2c.core.openapi-conventions")
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
