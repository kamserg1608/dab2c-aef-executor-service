plugins {
    java
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring") apply false
    id("org.jetbrains.kotlin.plugin.serialization") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
}

allprojects {
    group = "ru.sbrf.ufs.dab2c.core.executor"
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
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        resolutionStrategy {
            force("ch.qos.logback:logback-classic:1.2.12")
            force("ch.qos.logback:logback-core:1.2.12")
            force("org.slf4j:slf4j-api:1.7.36")
        }
    }
}

subprojects {
    apply(plugin = "java-library")

    dependencies {
        implementation(platform(rootProject.libs.ufs.platform.bom))

        // Core dependencies
        implementation(rootProject.libs.javax.annotation.api)
        implementation(rootProject.libs.kotlin.reflect)

        // Spring Core bundle
        implementation(rootProject.libs.bundles.spring.boot.web)
        implementation(rootProject.libs.bundles.spring.core)
        implementation(rootProject.libs.spring.boot.autoconfigure)

        // UFS Platform
        implementation(rootProject.libs.bundles.ufs.core)
        implementation(rootProject.libs.bundles.ufs.sup)

        //Other
        implementation(rootProject.libs.bundles.jackson)
        implementation(rootProject.libs.bundles.kotlinx)

        // Testing bundles
        testImplementation(rootProject.libs.bundles.testing)
    }

}
