import gradle.kotlin.dsl.accessors._285dcef16d8875fee0ec91e18e07daf9.implementation
import gradle.kotlin.dsl.accessors._285dcef16d8875fee0ec91e18e07daf9.testImplementation
import gradle.kotlin.dsl.accessors._285dcef16d8875fee0ec91e18e07daf9.testRuntimeOnly

plugins {
    id("ru.sbrf.ufs.dab2c.core.kotlin-conventions")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
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
