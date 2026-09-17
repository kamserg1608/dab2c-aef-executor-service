plugins {
    id("ru.sbrf.ufs.dab2c.core.deployment-conventions")
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Executor application"

springBoot {
    mainClass.set("ru.sbrf.dab2c.executor.application.ApplicationEntryPointKt")
}

dependencies {
    implementation(project(":executor-distribution"))
    implementation(project(":executor-services:voice-executor"))
    implementation(project(":executor-libraries:jackson"))
    implementation(project(":executor-clients:http-client-factory"))

    implementation(libs.springdoc.openapi.starter.webflux.ui)
    implementation(libs.aef.sdk.spring.boot.starter)
    implementation(libs.spring.boot.starter.actuator)

    // JSON logging for PROM profile
    implementation(libs.logstash.logback.encoder)
}
