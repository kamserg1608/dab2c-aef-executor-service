plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "commons"

dependencies {
    implementation(project(":executor-shared:annotations"))
    implementation(project(":executor-shared:rest-object-mapper"))
    testImplementation(project(":executor-shared:test-utils"))
}
