description = "cbul-integration"

dependencies {
    api(libs.ru.sbrf.cbul.cbul.client.spring.boot.starter)
    api(project(":executor-shared:commons"))
    api(project(":executor-shared:monitoring"))
    api(project(":executor-shared:circuit-breaker"))
    api(project(":executor-shared:annotations"))
    testImplementation(libs.org.springframework.boot.spring.boot.starter.test)
    testImplementation(project(":executor-shared:test-utils"))
}
