plugins {
    id("ru.sbrf.ufs.dab2c.core.grpc-conventions")
}

group = "ru.sbrf.ufs.dab2c.executor.clients"

dependencies {
    // Domain model
    api(project(":executor-domain-model"))

    // Coroutines for Flow
    implementation(libs.kotlinx.coroutines.core)

    // Stub server dependencies (only used when STUB-GV profile is active)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.kotlin.logging.jvm)
    compileOnly("org.springframework:spring-context")

    testImplementation(project(":executor-libraries:jackson"))
}
