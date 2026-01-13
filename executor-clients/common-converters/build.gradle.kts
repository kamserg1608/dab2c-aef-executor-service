plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Common converters for Konvert mappers"

dependencies {
    // Domain models
    api(project(":executor-domain-model"))

    // Protobuf types for converter utilities
    implementation(libs.protobuf.java)
}
