import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    java
    jacoco

    id("org.jetbrains.kotlin.plugin.spring") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("org.sonarqube")
}

allprojects {
    group = "ru.sbrf.ufs.dab2c.core"
    version = "1.0.0"

    repositories {
        val nexusUsername = System.getProperty("gradle.wrapperUser")
        val nexusPassword = System.getProperty("gradle.wrapperPassword")
        val protectedRepo = { repoUrl: String ->
            maven {
                credentials {
                    username = nexusUsername
                    password = nexusPassword
                }
                url = uri(repoUrl)
            }
        }

        mavenLocal()
        protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/public/")
        protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-int/")
        protectedRepo("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-release/")
    }

    configurations.all {
        // Exclude JUnit 4 (we use JUnit 5)
        exclude(group = "junit", module = "junit")
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    dependencies {
        // Spring Boot BOM for dependency management
        implementation(platform(rootProject.libs.spring.boot.dependencies))
        implementation(platform(rootProject.libs.jackson.bom))
        implementation(platform(rootProject.libs.netty.bom))

        constraints {
            implementation(rootProject.libs.lz4.java)
        }

        implementation(rootProject.libs.spring.boot.starter.webflux)
        implementation(rootProject.libs.jackson.module.kotlin)

        // Logging
        implementation(rootProject.libs.kotlin.logging.jvm)

        // Testing (includes JUnit 5, AssertJ, Mockito, etc.)
        testImplementation(rootProject.libs.spring.boot.starter.test)
        testImplementation(rootProject.libs.mockk)
        testImplementation(rootProject.libs.kotlinx.coroutines.test)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude(
                            "**/dto/**",
                            "**/model/**",
                            "**/factory/**",
                            "**/pojo/**",
                            "**/config/**",
                            "**/*AutoConfiguration.*",
                            "**/*Configuration.*",
                            "**/*Application.*",
                            "**/*ApplicationEntryPoint.*",
                            "**/*Constants.*",
                            "**/*Exception.*"
                        )
                    }
                }
            )
        )
    }

}

sonar {
    properties {
        property("sonar.host.url", "https://sbt-sonarqube.sigma.sbrf.ru")
        property("sonar.projectKey", "executor-java")
        property("sonar.projectName", "executor-java")
        property("sonar.sourceEncoding", "UTF-8")
//        gradle sonar
//        property("sonar.login", "sonar_token")
        property("sonar.scanner.socketTimeout", "300")
        property("sonar.scanner.skipSystemTruststore", "true")

        property(
            "sonar.exclusions",
            "**/build/**,**/generated/**,**/src/main/resources/mock/**"
        )
        property(
            "sonar.coverage.exclusions",
            "**/dto/**,**/model/**,**/factory/**,**/pojo/**,**/config/**," +
                "**/*AutoConfiguration.*,**/*Configuration.*,**/*Application.*," +
                "**/*ApplicationEntryPoint.*,**/*Constants.*,**/*Exception.*"
        )
        property(
            "sonar.cpd.exclusions",
            "**/dto/**,**/model/**,**/pojo/**"
        )
    }
}

// Sonar is opt-in: regular `build` does not run analysis.
// Build one report from every Test task (including executor-e2e-tests:test)
// and map its execution data to production classes from every subproject.
val testTasksByProject = subprojects.map { it.tasks.withType<Test>() }
val mainSourceSets = subprojects.map {
    it.extensions.getByType<SourceSetContainer>()["main"]
}

val aggregateJacocoReport = tasks.register<JacocoReport>("jacocoAggregateReport") {
    group = "verification"
    description = "Aggregates unit and E2E JaCoCo coverage for Sonar"

    testTasksByProject.forEach { tests ->
        dependsOn(tests)
        executionData(tests)
    }

    sourceDirectories.setFrom(mainSourceSets.flatMap { it.allSource.srcDirs })
    classDirectories.setFrom(
        mainSourceSets.map { sourceSet ->
            sourceSet.output.asFileTree.matching {
                exclude(
                    "**/dto/**",
                    "**/model/**",
                    "**/factory/**",
                    "**/pojo/**",
                    "**/config/**",
                    "**/*AutoConfiguration.*",
                    "**/*Configuration.*",
                    "**/*Application.*",
                    "**/*ApplicationEntryPoint.*",
                    "**/*Constants.*",
                    "**/*Exception.*"
                )
            }
        }
    )

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregate/jacoco.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregate/html"))
    }
}

tasks.named("sonar") {
    dependsOn(aggregateJacocoReport)
}
