rootProject.name = "dab2c-core"
include("executor-distribution")
include("executor-application")
include("executor-e2e-tests")
include("executor-clients:giga-voice")
include("executor-services:voice-executor")

pluginManagement {
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
}
