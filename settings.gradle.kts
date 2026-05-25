rootProject.name = "dab2c-executor"
include("executor-domain-model")
include("executor-distribution")
include("executor-application")
include("executor-e2e-tests")
include("executor-clients:common-converters")
include("executor-clients:giga-voice")
include("executor-clients:giga-agent")
include("executor-clients:efs-adapter")
include("executor-clients:kap-producer")
include("executor-clients:configurator")
include("executor-clients:iag")
include("executor-services:voice-executor")
include("executor-libraries:jackson")
include("executor-libraries:context")
include("executor-libraries:logging")
include("executor-libraries:monitoring")
include("executor-libraries:audit")
include("executor-libraries:time")
include("executor-libraries:test-support")

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
