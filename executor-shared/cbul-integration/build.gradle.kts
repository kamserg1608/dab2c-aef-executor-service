description = "cbul-integration"

dependencies {
    implementation(project(":executor-shared:commons"))
    implementation(project(":executor-shared:monitoring"))
    implementation(project(":executor-shared:circuit-breaker"))
    implementation(project(":executor-shared:annotations"))
    testImplementation(project(":executor-shared:test-utils"))
}
