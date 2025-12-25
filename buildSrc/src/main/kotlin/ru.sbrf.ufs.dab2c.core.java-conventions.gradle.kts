plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
    id("org.gradle.checkstyle")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    val lombokLibrary = libs.findLibrary("lombok").get().get()
    compileOnly(lombokLibrary)
    annotationProcessor(lombokLibrary)

    // For test code
    testCompileOnly(lombokLibrary)
    testAnnotationProcessor(lombokLibrary)
}

checkstyle {
    toolVersion = "9.3"
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false
    maxWarnings = 0
    maxErrors = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
