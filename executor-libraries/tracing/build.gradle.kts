plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(libs.aef.sdk.spring.boot.starter)
    api(libs.aef.sdk.voice)
    api(libs.opentelemetry.extension.kotlin)
    implementation(project(":executor-domain-model"))
    implementation(project(":executor-libraries:context"))
    implementation(project(":executor-libraries:jackson"))
    implementation(libs.kotlin.logging.jvm)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.server.spring.boot.starter)
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.testing)
}
