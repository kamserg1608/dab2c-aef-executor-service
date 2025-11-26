plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
}

description = "circuit-breaker"

dependencies {
    implementation(libs.resilience4j.circuitbreaker)
    implementation(project(":executor-shared:logging"))
    implementation(project(":executor-shared:commons"))
    testImplementation(project(":executor-shared:test-utils"))
}
