description = "concurrent"

dependencies {
    api(libs.ru.sbrf.ufs.platform.environment.spring.boot.starter)
    api(libs.ru.sbrf.ufs.platform.logger.springboot.starter)
    api(libs.ru.sbrf.ufs.monitoring.spring.boot.monitoring.spring.boot.starter)
    api(project(":executor-shared:commons"))
    testImplementation(project(":executor-shared:test-utils"))
    testImplementation(libs.org.mockito.mockito.core)
}
