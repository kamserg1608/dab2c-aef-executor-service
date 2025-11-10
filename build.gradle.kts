plugins {
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
}
