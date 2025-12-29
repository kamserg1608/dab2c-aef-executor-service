plugins {
    id("ru.sbrf.ufs.dab2c.core.deployment-conventions")
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

description = "Integration-layer application"

springBoot {
    mainClass.set("ru.sbrf.ufs.dab2c.core.ApplicationEntryPoint")
}

dependencies {
    implementation(project(":executor-distribution"))
    implementation(project(":executor-libraries:sup"))

    implementation(libs.bundles.ufs.healthcheck)
    implementation(libs.bundles.ufs.monitoring)
}
