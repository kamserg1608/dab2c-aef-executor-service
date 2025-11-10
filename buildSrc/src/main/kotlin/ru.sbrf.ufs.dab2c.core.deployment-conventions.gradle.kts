plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Access version catalog
val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    // Spring Boot starters for the application
    implementation(libs.findBundle("spring.boot.web").get())
    implementation(libs.findBundle("spring.security").get())
    implementation(libs.findBundle("springdoc").get())

    // Ktor BOM for client dependencies used by services
    implementation(platform(libs.findLibrary("ktor.bom").get()))

    // Test dependencies
    testImplementation(libs.findBundle("testing").get())
    testRuntimeOnly(libs.findLibrary("junit.platform.launcher").get())
}
