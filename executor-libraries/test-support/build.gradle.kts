plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Shared test helpers — assertions, fixtures, and other testing utilities"

dependencies {
    api(project(":executor-libraries:jackson"))
    api(libs.opentelemetry.proto)
    api(libs.spring.boot.starter.test)
}
