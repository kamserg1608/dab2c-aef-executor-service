def onDistrib(app, distr) {

    def skipTests = env.SKIP_TEST?.toBoolean() ? " -x test " : ""
    def skipStaticChecks =  env.SKIP_STATIC_CHECKS?.toBoolean()?:false
    gradlew("clean :executor-shared:test-utils:dependencies build", skipTests)

    String deploymentUnit1 = "router-executor"
    distr.addConf("./executor-application/configs/build/resources/main/*")
    distr.addBH("./executor-application/deployments/${deploymentUnit1}/build/zero-compressed-main-jar/*.jar", "${deploymentUnit1}-main")
    distr.addBH("./executor-application/deployments/${deploymentUnit1}/build/zero-compressed-dependencies-jars/*.jar", "${deploymentUnit1}-dependencies")
}

void gradlew(String task, String options) {
    withPreparedEnv("openjdk-21") {
        withCredentials([usernamePassword(
                credentialsId: 'nexus_ci_cred',
                passwordVariable: 'wrappedPassword',
                usernameVariable: 'wrappedUser')]) {
            String wrapperOptions = getWrapperOptions(wrappedUser, wrappedPassword)
            sh "./gradlew ${wrapperOptions} ${task} ${options}"
        }
    }
}

void withPreparedEnv(String jdk, Closure closure) {
    withEnv([
            "JAVA_HOME=${tool name: jdk, type: 'jdk'}",
    ]) {
        closure.call()
    }
}

String getWrapperOptions(String wrappedUser, String wrappedPassword) {
    return [
            "-Dgradle.wrapperUser=${wrappedUser}",
            "-Dgradle.wrapperPassword='${wrappedPassword}'",
            "-Dgradle.wrappedUser=${wrappedUser}",
            "-Dgradle.wrappedPassword='${wrappedPassword}'",
    ].join(" ")
}

return wrapJenkinsfile(this)
