import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.kotlin.noarg)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.versions)
    `java-library`
    jacoco
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
}

subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.allopen")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "org.jetbrains.kotlin.plugin.noarg")
    apply(plugin = "org.jetbrains.kotlin.plugin.jpa")
    apply(plugin = "io.spring.dependency-management")
//    apply(plugin = "com.github.spotbugs")
    apply(plugin = "java-library")
//    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "jacoco")

    dependencies {
        implementation(platform(rootProject.libs.ufs.platform.bom))
        implementation(platform(rootProject.libs.junit.bom))

        kapt(rootProject.libs.mapstruct.processor)

        implementation(rootProject.libs.javax.annotation.api)
        implementation(rootProject.libs.spotbugs.annotations)
        implementation(rootProject.libs.slf4j.log4j12)

        implementation(rootProject.libs.bundles.kotlin)
        implementation(rootProject.libs.bundles.spring.core)

        implementation(rootProject.libs.ufs.platform.config.api)
        implementation(rootProject.libs.ufs.platform.config.core)
        implementation(rootProject.libs.ufs.platform.api)
        implementation(rootProject.libs.ufs.platform.json.mapper)
        implementation(rootProject.libs.spring.boot.autoconfigure)
        implementation(rootProject.libs.mapstruct)

        testImplementation(rootProject.libs.bundles.test)
        testImplementation(rootProject.libs.bundles.junit)
        testImplementation(rootProject.libs.wiremock)
        testImplementation(rootProject.libs.mockk.jvm)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile>() {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc>() {
        options.encoding = "UTF-8"
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all-compatibility")
        }
    }

    jacoco {
        toolVersion = "0.8.12"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            xml.outputLocation.set(file("$buildDir/site/jacoco/jacoco.xml"))
            html.required.set(true)
        }
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    allOpen {
        annotations(
            "org.springframework.stereotype.Component",
            "org.springframework.stereotype.Service",
            "org.springframework.stereotype.Repository",
            "org.springframework.stereotype.Controller",
            "org.springframework.stereotype.RestController",
            "org.springframework.stereotype.Configuration",
            "org.springframework.boot.context.properties.ConfigurationProperties",
            "javax.persistence.Entity",
            "javax.persistence.MappedSuperclass",
            "javax.persistence.Embeddable"
        )
    }

    noArg {
        annotations(
            "javax.persistence.Entity",
            "javax.persistence.MappedSuperclass",
            "javax.persistence.Embeddable",
            "kotlinx.serialization.Serializable"
        )
    }

    kapt {
        correctErrorTypes = true
        arguments {
            arg("mapstruct.defaultComponentModel", "spring")
            arg("mapstruct.unmappedTargetPolicy", "IGNORE")
        }
    }
}
