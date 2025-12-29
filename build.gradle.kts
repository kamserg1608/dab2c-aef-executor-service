plugins {
    java
    id("org.jetbrains.kotlin.plugin.spring") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
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
        exclude(group = "junit", module = "junit")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit")
        resolutionStrategy {
            force("ch.qos.logback:logback-classic:1.2.12")
            force("ch.qos.logback:logback-core:1.2.12")
            force("org.slf4j:slf4j-api:1.7.36")
            force("ru.sbrf.ufs.platform:httpclient-configuration-processor:7.23.15.0")
            force(
                "org.junit.platform:junit-platform-launcher:1.8.2",
                "org.junit.platform:junit-platform-engine:1.8.2",
                "org.junit.platform:junit-platform-commons:1.8.2",
                "org.junit.platform:junit-platform-suite-api:1.8.2",
                "org.junit.jupiter:junit-jupiter-api:5.8.2",
                "org.junit.jupiter:junit-jupiter-engine:5.8.2",
                "org.junit.jupiter:junit-jupiter-params:5.8.2",
                "org.opentest4j:opentest4j:1.2.0"
            )
            force (
                "io.micrometer:micrometer-core:1.9.17",
                "io.micrometer:micrometer-registry-prometheus:1.9.17",
            )
        }
    }
}

subprojects {
    apply(plugin = "java-library")

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.8.2"))
        implementation(platform(rootProject.libs.ufs.platform.bom))

        // Core dependencies
        implementation(rootProject.libs.javax.annotation.api)
        implementation(rootProject.libs.javax.validation.api)

        // Spring Core bundle
        implementation(rootProject.libs.bundles.spring.boot.web)

        // UFS Platform
        implementation(rootProject.libs.bundles.ufs.core)
        implementation(rootProject.libs.bundles.ufs.logging)

        //Other
        implementation(rootProject.libs.bundles.jackson)

        // Testing bundles
        testImplementation(rootProject.libs.bundles.testing)
    }

}
