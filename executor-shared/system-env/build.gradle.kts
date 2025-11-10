plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "system-env"

dependencies {
    implementation(project(":executor-shared:annotations"))
}
