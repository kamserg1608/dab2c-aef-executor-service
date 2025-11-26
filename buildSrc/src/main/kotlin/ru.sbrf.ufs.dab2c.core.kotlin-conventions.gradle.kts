import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("io.gitlab.arturbosch.detekt")
    id("com.github.spotbugs")
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

spotbugs {
    toolVersion.set("4.9.3")
    ignoreFailures.set(false)
    excludeFilter.set(file("${project.rootDir}/config/spotbugs/excludeFilters.xml"))
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
    spotbugs("com.github.spotbugs:spotbugs:4.9.3")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.9.3")
}
