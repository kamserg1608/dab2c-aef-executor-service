import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }
}

detekt {
    toolVersion = "1.23.7"
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
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
}
