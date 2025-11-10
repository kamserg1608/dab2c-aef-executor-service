plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "monitoring"

dependencies {
    api(libs.bundles.ufs.monitoring)
    implementation(project(":executor-shared:annotations"))
    implementation(project(":executor-shared:logging"))
    implementation(project(":executor-shared:commons"))
    testImplementation(project(":executor-shared:test-utils"))
}
