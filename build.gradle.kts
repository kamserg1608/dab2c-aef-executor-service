plugins {
    java
    id("org.jetbrains.kotlin.plugin.spring") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
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

    dependencies {
        // Spring Boot BOM for dependency management
        implementation(platform(rootProject.libs.spring.boot.dependencies))

        // Spring Boot Web (includes Tomcat, Jackson, validation, etc.)
        implementation(rootProject.libs.spring.boot.starter.web)
        implementation(rootProject.libs.spring.boot.starter.aop)
        implementation(rootProject.libs.jackson.module.kotlin)

        // Testing (includes JUnit 5, AssertJ, Mockito, etc.)
        testImplementation(rootProject.libs.spring.boot.starter.test)
        testImplementation(rootProject.libs.mockk)
        testImplementation(rootProject.libs.kotlinx.coroutines.test)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }
}
