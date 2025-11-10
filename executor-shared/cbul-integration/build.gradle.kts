plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "cbul-integration"

dependencies {
    api(libs.bundles.ufs.cbul)
    implementation(project(":executor-shared:commons"))
    implementation(project(":executor-shared:monitoring"))
    implementation(project(":executor-shared:circuit-breaker"))
    implementation(project(":executor-shared:annotations"))
    implementation(project(":executor-shared:logging"))
    testImplementation(project(":executor-shared:test-utils"))
}
