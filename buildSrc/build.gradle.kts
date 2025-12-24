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
    implementation(libs.spring.boot.gradle.plugin)
    implementation(gradleApi())

    // Explicitly add Kotlin plugins for use in convention plugins
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.10")

    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotbugs.gradle.plugin)

    // Zero-compress plugin for deployment conventions
    implementation("ru.sbrf.ufs.zero-compress-plugin:gradle-plugin:1.0.5")
    implementation(libs.protobuf.gradle.plugin)
}
