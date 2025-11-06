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
    implementation(libs.ru.sbrf.ufs.platform.environment.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.platform.ufs.platform.cache.impl)
    implementation(libs.ru.sbrf.ufs.platform.httpclient.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.platform.ufs.platform.config.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.platform.config.agent.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.app.hotreload.hotreload.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.platform.logger.springboot.starter)
    implementation(libs.ru.sbrf.ufs.monitoring.spring.boot.monitoring.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.healthcheck.ufs.healthcheck.spring.boot.starter)
    implementation(libs.ru.sbrf.ufs.platform.rest.app.jersey.spring.boot.starter)
    implementation(libs.com.networknt.json.schema.validator)
    implementation(project(":executor-shared:monitoring"))
    implementation(project(":executor-shared:circuit-breaker"))
    implementation(project(":executor-shared:logging"))
    implementation(project(":executor-shared:concurrent"))
    implementation(project(":executor-shared:rest-object-mapper"))
    testImplementation(project(":executor-shared:test-utils"))
}
