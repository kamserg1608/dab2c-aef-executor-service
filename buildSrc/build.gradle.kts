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

    // Explicitly add Kotlin plugins for use in convention plugins
    implementation("org.jetbrains.kotlin:kotlin-allopen:2.1.0")
    implementation("org.jetbrains.kotlin:kotlin-serialization:2.1.0")
}
