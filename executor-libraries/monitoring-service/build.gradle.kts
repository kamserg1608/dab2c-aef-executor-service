plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
}

dependencies {
    api(libs.micrometer.registry.prometheus)
    implementation(project(":executor-libraries:context"))
    implementation(libs.spring.boot.starter.actuator)
}
