plugins {
    id("ru.sbrf.ufs.dab2c.core.grpc-conventions")
}

group = "ru.sbrf.ufs.dab2c.executor.clients"

dependencies {
    // Domain model
    api(project(":executor-domain-model"))

    // Common converters for type mapping
    implementation(project(":executor-clients:common-converters"))

    // Coroutines for Flow
    implementation(libs.kotlinx.coroutines.core)
}
