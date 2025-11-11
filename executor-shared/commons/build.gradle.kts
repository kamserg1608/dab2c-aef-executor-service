plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "commons"

dependencies {
    implementation(project(":executor-shared:annotations"))
    testImplementation(project(":executor-shared:test-utils"))
}
