import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("ru.sbrf.ufs.dab2c.core.shared-conventions")
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("io.gitlab.arturbosch.detekt")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

dependencies {
    implementation(libs.findBundle("kotlinx").get())
}

detekt {
    ignoreFailures = true
    toolVersion = libs.findVersion("detekt").get().toString()
    config.setFrom("${project.rootDir}/config/detekt/detekt_config.yml")
    buildUponDefaultConfig = true
    allRules = false

    source.setFrom(
        files(
            "src/main/kotlin",
            "src/test/kotlin"
        )
    )
}

dependencies {
    detektPlugins(libs.findLibrary("detekt-formatting").get())
}

// Force Kotlin version for detekt to match its compiled version
configurations.matching { it.name.startsWith("detekt") }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}
