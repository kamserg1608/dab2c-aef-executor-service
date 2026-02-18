def onDistrib(app, distr) {

    def skipTests = env.SKIP_TEST?.toBoolean() ? " -x test " : ""
    def version = "-Pdistrib.version=${distr.version}"
    gradlew("clean build", "${version} ${skipTests}")

    distr.addConf("./executor-distribution/build/resources/main/distr/*")

    distr.addBH("./executor-application/build/zero-compressed-main-jar/*.jar", "executor-main")
    distr.addBH("./executor-application/build/zero-compressed-dependencies-jars/*.jar", "executor-dependencies")
}

void gradlew(String task, String options) {
    withPreparedEnv("sberjdk-21") {
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
