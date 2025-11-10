plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "health-check"

dependencies {
    api(libs.bundles.ufs.healthcheck)
    implementation(project(":executor-shared:logging"))
}
