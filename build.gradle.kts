import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.spring.dependency.management)
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
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "java-library")
    apply(plugin = "jacoco")

    dependencies {
        implementation(platform(rootProject.libs.junit.bom))
        implementation(platform(rootProject.libs.ufs.platform.bom))
        implementation(rootProject.libs.bundles.kotlin)
        implementation(rootProject.libs.bundles.spring.core)
        implementation(rootProject.libs.bundles.test)
        implementation(rootProject.libs.com.fasterxml.jackson.datatype.jackson.datatype.jsr310)
        implementation(rootProject.libs.com.fasterxml.jackson.module.jackson.module.kotlin)
        implementation(rootProject.libs.com.jayway.jsonpath.json.path)
        implementation(rootProject.libs.com.networknt.json.schema.validator)
        implementation(rootProject.libs.io.github.resilience4j.resilience4j.circuitbreaker)
        implementation(rootProject.libs.javax.annotation.api)
        implementation(rootProject.libs.mapstruct)
        implementation(rootProject.libs.org.apache.httpcomponents.httpclient)
        implementation(rootProject.libs.org.apache.httpcomponents.httpcore)
        implementation(rootProject.libs.org.apache.kafka.kafka.clients)
        implementation(rootProject.libs.org.slf4j.slf4j.api)
        implementation(rootProject.libs.org.springframework.boot.spring.boot)
        implementation(rootProject.libs.org.springframework.spring.webmvc)
        implementation(rootProject.libs.ru.sbrf.cbul.cbul.client.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.app.hotreload.hotreload.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.cbreaker.circuit.breaker.api)
        implementation(rootProject.libs.ru.sbrf.ufs.healthcheck.ufs.healthcheck.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.monitoring.spring.boot.monitoring.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.config.agent.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.environment.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.httpclient.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.logger.api)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.logger.springboot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.rest.app.jersey.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.ufs.platform.cache.impl)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.ufs.platform.config.spring.boot.starter)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.ufs.platform.core)
        implementation(rootProject.libs.ru.sbrf.ufs.platform.ufs.platform.healthcheck.api)
        implementation(rootProject.libs.slf4j.log4j12)
        implementation(rootProject.libs.spotbugs.annotations)
        implementation(rootProject.libs.spring.boot.autoconfigure)
        implementation(rootProject.libs.ufs.platform.api)
        implementation(rootProject.libs.ufs.platform.config.api)
        implementation(rootProject.libs.ufs.platform.config.core)
        implementation(rootProject.libs.ufs.platform.json.mapper)
        kapt(rootProject.libs.mapstruct.processor)
        testImplementation(rootProject.libs.bundles.junit)
        testImplementation(rootProject.libs.bundles.test)
        testImplementation(rootProject.libs.mockk.jvm)
        testImplementation(rootProject.libs.org.mockito.mockito.core)
        testImplementation(rootProject.libs.org.springframework.boot.spring.boot.starter.test)
        testImplementation(rootProject.libs.wiremock)
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<Javadoc> {
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
}
