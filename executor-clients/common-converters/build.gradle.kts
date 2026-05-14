plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Common converters for Konvert mappers"

dependencies {
    api(project(":executor-libraries:monitoring"))

    // Protobuf types for converter utilities
    implementation(libs.protobuf.java)
}
