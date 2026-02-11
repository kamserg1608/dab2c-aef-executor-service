rootProject.name = "dab2c-executor"
include("executor-domain-model")
include("executor-distribution")
include("executor-application")
include("executor-e2e-tests")
include("executor-clients:common-converters")
include("executor-clients:giga-voice")
include("executor-clients:giga-agent")
include("executor-clients:efs-adapter")
include("executor-clients:ivr-voice")
include("executor-clients:kap-producer")
include("executor-services:voice-executor")
include("executor-libraries:logging")
include("executor-libraries:monitoring-service")
include("executor-libraries:audit-service")
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
