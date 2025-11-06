import io.gitlab.arturbosch.detekt.Detekt
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
    alias(libs.plugins.detekt)
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.versions)
    jacoco
}

allprojects {
    group = "ru.sbrf.ufs.dab2c.core.executor"
    version = "1.0.0"

    apply(plugin = "buildlogic.java-conventions")

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

val javaVersion = JavaVersion.VERSION_21

sourceSets {
    main {
        java {
            srcDirs("src/main/kotlin", "src/main/java")
        }
    }
    test {
        java {
            srcDirs("src/test/kotlin", "src/test/java")
        }
    }
}

dependencies {
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

    implementation(platform(rootProject.libs.ufs.platform.bom))
    implementation(platform(rootProject.libs.junit.bom))

    kapt(rootProject.libs.mapstruct.processor)
    implementation(rootProject.libs.mapstruct)

    testImplementation(rootProject.libs.bundles.test)
    testImplementation(rootProject.libs.wiremock)
    testImplementation(rootProject.libs.mockk.jvm)
}

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all-compatibility")
        }
    }
//
//    configurations {
//        all {
//            exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
//        }
//    }
//
//
    detekt {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom("$rootDir/config/detekt/detekt_config.yml")
        autoCorrect = true
    }

    tasks.withType<Detekt>().configureEach {
        reports {
            xml.required.set(true)
            xml.outputLocation.set(file("$buildDir/detekt/detekt.xml"))
            html.required.set(true)
            txt.required.set(false)
        }
        exclude("**/resources/**")
    }

//    spotbugs {
//        effort.set(com.github.spotbugs.snom.Effort.MAX)
//        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
//        excludeFilter.set(file("$rootDir/config/spotbugs/excludeFilters.xml"))
//        includeFilter.set(null)
//    }
//
//    tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
//        reports.create("xml") {
//            required.set(true)
//            outputLocation.set(file("$buildDir/spotbugsXml.xml"))
//        }
//    }

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

sonarqube {
    properties {
        property("sonar.host.url", "https://sbt-sonarqube.sigma.sbrf.ru")
        property("sonar.login", "sqp_85bd535d881cea6171d59e505ab60c506cd4a505")
        property("sonar.ws.timeout", "300")
        property("sonar.coverage.exclusions", """
            **/dto/**,
            **/factory/**,
            **/pojo/**,
            **/*Exception.*,
            **/config/**,
            **/*Configuration.*,
            **/model/**,
            **/DefaultAutoConfigurationExcludeProvider.kt,
            **/SpykBeanConfig.kt,
            **/HttpResponseExt.kt,
            **/testutils/**,
            **/*Stub.kt,
            **/*EntryPoint.kt,
            **/*Exception*.kt,
            **/*ServiceHostProvider.kt
        """.trimIndent())
        property("sonar.coverage.jacoco.xmlReportPaths", "**/build/site/jacoco/jacoco.xml")
        property("sonar.java.checkstyle.reportPaths", "**/build/checkstyle-result.xml")
        property("sonar.kotlin.detekt.reportPaths", "**/build/detekt/detekt.xml")
        property("sonar.java.spotbugs.reportPaths", "**/build/spotbugsXml.xml")
        property("sonar.surefire.reportsPath", "**/build/test-results/test")
        property("sonar.java.source", "21")
        property("sonar.java.target", "21")
        property("sonar.sources", "src/main")
    }
}

tasks.register("buildAll") {
    dependsOn(subprojects.map { it.tasks.build })
}

val skipAllChecks: String by project

tasks.withType<Detekt>().configureEach {
    enabled = skipAllChecks != "true"
}

//tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
//    enabled = skipAllChecks != "true"
//}

tasks.withType<JacocoReport>().configureEach {
    enabled = skipAllChecks != "true"
}