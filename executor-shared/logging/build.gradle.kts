plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "logging"

dependencies {
    implementation(project(":executor-shared:commons"))
    testImplementation(project(":executor-shared:test-utils"))
}
