plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Shared Ktor HTTP client factory and HTTP client utilities"

dependencies {
    api(project(":executor-libraries:monitoring"))
    api(project(":executor-libraries:tracing"))
    api(project(":executor-libraries:jackson"))
    api(libs.bundles.ktor.client)
}
