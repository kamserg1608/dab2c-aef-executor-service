plugins {
    alias(libs.plugins.spring.boot)
}

description = "router-executor"

springBoot {
    mainClass.set("ru.sbrf.ufs.dab2c.core.executor.deployments.router.executor.app.RouterExecutorApplicationEntryPointKt")
}

dependencies {
    implementation(project(":executor-shared:health-check"))
    implementation(project(":executor-shared:commons"))
    implementation(project(":executor-shared:cbul-integration"))
    implementation(project(":executor-shared:monitoring"))
    implementation(project(":executor-shared:circuit-breaker"))
    implementation(project(":executor-shared:logging"))
    implementation(project(":executor-shared:concurrent"))
    implementation(project(":executor-shared:rest-object-mapper"))
    testImplementation(project(":executor-shared:test-utils"))
}
