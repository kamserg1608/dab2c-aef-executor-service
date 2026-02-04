plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Common converters for Konvert mappers"

dependencies {
    // Domain models
    api(project(":executor-domain-model"))
    api(project(":executor-libraries:monitoring-service"))

    // Protobuf types for converter utilities
    implementation(libs.protobuf.java)
}
