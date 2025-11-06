

dependencies {
    api(libs.ru.sbrf.ufs.platform.environment.spring.boot.starter)
    api(libs.ru.sbrf.ufs.platform.logger.springboot.starter)
    api(project(":executor-shared:commons"))
    api(libs.io.github.resilience4j.resilience4j.circuitbreaker)
    testImplementation(project(":executor-shared:test-utils"))
}

description = "circuit-breaker"
