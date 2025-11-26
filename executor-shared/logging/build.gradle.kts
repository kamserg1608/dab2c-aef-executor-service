plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "logging"

dependencies {
    api(libs.bundles.ufs.logging)
    implementation(project(":executor-shared:commons"))
    testImplementation(project(":executor-shared:test-utils"))
}
