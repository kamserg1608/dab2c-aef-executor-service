plugins {
    `kotlin-dsl`
}

repositories {
    val nexusUsername = System.getProperty("gradle.wrapperUser")
    val nexusPassword = System.getProperty("gradle.wrapperPassword")
    maven {
        credentials {
            username = nexusUsername
            password = nexusPassword
        }
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/public/")
    }
    maven {
        credentials {
            username = nexusUsername
            password = nexusPassword
        }
        url = uri("https://nexus-ci.delta.sbrf.ru/repository/maven-lib-int/")
    }
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.allopen)
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)

    implementation(libs.protobuf.gradle.plugin)
    implementation(libs.openapi.generator.gradle.plugin)
    implementation(libs.ksp.gradle.plugin)
    implementation(gradleApi())

    // Zero-compress plugin for deployment conventions
    implementation("ru.sbrf.ufs.zero-compress-plugin:gradle-plugin:1.0.5")
}
